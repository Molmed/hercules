package hercules.actors.processingunitwatcher

import java.io.File
import java.io.FileNotFoundException

import scala.collection.JavaConversions._
import scala.concurrent.duration.DurationInt

import com.typesafe.config.ConfigFactory

import akka.actor.Props
import akka.actor.actorRef2Scala
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
  def receive = {

    case CheckForRunfolders => {
      log.info("Looking for new runfolders!")

      val fetcherConfig = new IlluminaProcessingUnitFetcherConfig(
        config.runfolderRootPaths.map(x => new File(x)),
        new File(config.samplesheetPath),
        new File(config.qcControlConfigPath),
        new File(config.defaultQCConfigFile),
        new File(config.programConfigPath),
        new File(config.defaultProgramConfigFile),
        log)

      try {
        def result =
          fetcher.checkForReadyProcessingUnits(fetcherConfig)

        self ! ProcessingUnitSequenceMessage(result)

      } catch {
        case e @ (_: FileNotFoundException | _: IllegalArgumentException | _: AssertionError | _: Exception) => {
          notice.warning("Failed with " + e.getClass.getSimpleName + " when checking for ready processing units: " + e.getMessage)
          throw e
        }
      }
    }
    case ProcessingUnitSequenceMessage(seq) => {
      for (unit <- seq)
        context.parent ! HerculesMainProtocol.FoundProcessingUnitMessage(unit)
    }
  }

}