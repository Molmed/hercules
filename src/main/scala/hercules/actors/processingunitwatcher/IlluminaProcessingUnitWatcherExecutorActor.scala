package hercules.actors.processingunitwatcher

import java.io.File
import java.io.FileNotFoundException
import scala.collection.JavaConversions._
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.{ LoggingAdapter, LoggingReceive }
import akka.pattern.pipe
import hercules.actors.HerculesActor
import hercules.config.processing.IlluminaProcessingUnitWatcherConfig
import hercules.config.processingunit.IlluminaProcessingUnitFetcherConfig
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.entities.illumina.IlluminaProcessingUnitFetcher
import hercules.protocols.HerculesMainProtocol

object IlluminaProcessingUnitWatcherExecutorActor {

  /**
   * Load the default config values for from the application.conf file.
   * @return the default IlluminaProcessingUnitWatcherConfig
   */
  def createDefaultConfig(): IlluminaProcessingUnitWatcherConfig = {

    val generalConfig = ConfigFactory.load()
    val conf = generalConfig.getConfig("remote.actors").withFallback(generalConfig)

    val runfolderPaths = conf.getStringList("general.runFolderPath").toList
    val samplesheetPath = conf.getString("general.samplesheetPath")

    val customQCConfigurationRoot = conf.getString("general.customQCConfigurationFilesRoot")
    val defaultQCConfigFile = conf.getString("general.defaultQCConfigFile")

    val customProgamConfigurationRoot = conf.getString("general.customProgramConfigFilesRoot")
    val defaultProgramConfigurationFile = conf.getString("general.defaultProgramConfigFile")
    val interval = conf.getInt("general.checkForRunfoldersInterval")

    val config = new IlluminaProcessingUnitWatcherConfig(runfolderPaths,
      samplesheetPath,
      customQCConfigurationRoot,
      defaultQCConfigFile,
      customProgamConfigurationRoot,
      defaultProgramConfigurationFile,
      interval)

    config
  }

  /**
   *  Create a IlluminaProcessingUnitFetcherConfig based on the supplied
   *  IlluminaProcessingUnitWatcherConfig and LoggingAdapter
   *  @param watcherConfig
   *  @param loggingAdapter
   *  @return A new IlluminaProcessingUnitFetcherConfig
   */
  def fetcherConfig(watcherConfig: IlluminaProcessingUnitWatcherConfig, log: LoggingAdapter): IlluminaProcessingUnitFetcherConfig =
    new IlluminaProcessingUnitFetcherConfig(
      watcherConfig.runfolderRootPaths.map(x => new File(x)),
      new File(watcherConfig.samplesheetPath),
      new File(watcherConfig.qcControlConfigPath),
      new File(watcherConfig.defaultQCConfigFile),
      new File(watcherConfig.programConfigPath),
      new File(watcherConfig.defaultProgramConfigFile),
      log)

  /**
   * Factory method for creating a IlluminaProcessingUnitExecutorActor
   * Loads it's configuration from the IlluminaProcessingUnitExecutorActor.conf
   * @param fetcher The type of fetcher to use to get the processing units
   * @return a Props of IlluminaProcessingUnitExecutorActor
   */
  def props(
    fetcher: IlluminaProcessingUnitFetcher = new IlluminaProcessingUnitFetcher(),
    getConfig: () => IlluminaProcessingUnitWatcherConfig = createDefaultConfig): Props = {

    val config = createDefaultConfig()
    Props(new IlluminaProcessingUnitWatcherExecutorActor(config, fetcher))
  }

  object IlluminaProcessingUnitWatcherExecutorActorProtocol {
    case class ProcessingUnitSequenceMessage(seq: Seq[IlluminaProcessingUnit])
    case object CheckForRunfolders
  }
}

/**
 * A actor which executes the checkForReadyProcessingUnits defined in the fetcher
 * at interval of X, and pass any processing units it finds on to the parent as
 * a FoundProcessingUnitMessage.
 *
 * @param config
 * @param fetcher
 */
class IlluminaProcessingUnitWatcherExecutorActor(
  config: IlluminaProcessingUnitWatcherConfig,
  fetcher: IlluminaProcessingUnitFetcher)
    extends HerculesActor with ProcessingUnitWatcherActor {

  import IlluminaProcessingUnitWatcherExecutorActor.IlluminaProcessingUnitWatcherExecutorActorProtocol._
  import HerculesMainProtocol._
  import context.dispatcher

  val checkForRunfolder =
    context.system.scheduler.schedule(1.seconds, config.checkForRunfoldersInterval.seconds, self, {
      CheckForRunfolders
    })

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    checkForRunfolder.cancel()
  }

  // Just pass the message on to the parent (the IlluminaProcessingUnitWatcherActor)
  def receive = LoggingReceive {

    case CheckForRunfolders => {
      log.debug("Looking for new runfolders!")

      val fetcherConfig = IlluminaProcessingUnitWatcherExecutorActor.fetcherConfig(config, log)

      try {
        def result =
          fetcher.checkForReadyProcessingUnits(fetcherConfig)

        self ! ProcessingUnitSequenceMessage(result)

      } catch {
        case e @ (_: FileNotFoundException | _: IllegalArgumentException | _: AssertionError | _: Exception) => {
          notice.warning("Failed with " + e.getClass.getSimpleName + " when checking for ready processing units: " + e.getMessage)
          log.error("Failed with " + e.getClass.getSimpleName + " when checking for ready processing units: " + e.getMessage)
          throw e
        }
      }
    }
    case ProcessingUnitSequenceMessage(seq) => {
      for (unit <- seq) {
        notice.info("New processingunit found: " + unit.name)
        context.parent ! HerculesMainProtocol.FoundProcessingUnitMessage(unit)
      }
    }

    case ForgetProcessingUnitMessage(unitName) => {
      val fetcherConfig = IlluminaProcessingUnitWatcherExecutorActor.fetcherConfig(config, log)
      // Attempt to fetch a IlluminaProcessingUnit corresponding to the supplied ProcessingUnitPlaceholder 
      Future {
        val hit: Option[IlluminaProcessingUnit] = fetcher.searchForProcessingUnitName(unitName, fetcherConfig)
        if (hit.nonEmpty) {
          hit.get.markNotFound
          if (!hit.get.isFound) Acknowledge
          else Reject(Some(s"ProcessingUnit corresponding to $unitName was found but could not be undiscovered"))
        } else {
          Reject(Some(s"Could not locate ProcessingUnit corresponding to $unitName"))
        }
      }.recover {
        case e: Exception =>
          Reject(Some("IlluminaProcessingUnitWatcherExecutorActor encountered exception while processing ForgetProcessingUnitMessage: " + e.getMessage))
      }.pipeTo(context.parent)
    }
  }

}