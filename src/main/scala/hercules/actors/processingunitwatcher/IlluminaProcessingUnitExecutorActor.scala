package hercules.actors.processingunitwatcher

import java.io.File
import com.typesafe.config.ConfigFactory
import akka.actor.Props
import hercules.actors.HerculesActor
import hercules.entities.illumina.IlluminaProcessingUnit
import scala.concurrent.duration._
import hercules.protocols.HerculesMainProtocol

object IlluminaProcessingUnitExecutorActor {

  /**
   * Factory method for creating a IlluminaProcessingUnitExecutorActor
   * Loads it's configuration from the IlluminaProcessingUnitExecutorActor.conf
   * @param configFile the configFile to load
   * @returns a Props of IlluminaProcessingUnitExecutorActor
   */
  def props(configFile: String = "IlluminaProcessingUnitExecutorActor"): Props = {

    val conf = ConfigFactory.load(configFile)
    val runfolderPath = conf.getString("runFolderPath")
    val samplesheetPath = conf.getString("samplesheetPath")

    val customQCConfigurationRoot = conf.getString("customQCConfigurationFilesRoot")
    val defaultQCConfigFile = conf.getString("defaultQCConfigFile")

    val customProgamConfigurationRoot = conf.getString("defaultQCConfigFile")
    val defaultProgramConfigurationFile = conf.getString("defaultProgramConfigFile")

    Props(new IlluminaProcessingUnitExecutorActor(
      runfolderPath,
      samplesheetPath,
      customQCConfigurationRoot,
      defaultQCConfigFile,
      customProgamConfigurationRoot,
      defaultProgramConfigurationFile))
  }
}

class IlluminaProcessingUnitExecutorActor(
    val runfolderRootPath: String,
    val samplesheetPath: String,
    val qcControlConfigPath: String,
    val defaultQCConfigFile: String,
    val programConfigPath: String,
    val defaultProgramConfigFile: String) extends HerculesActor with ProcessingUnitWatcherActor {

  import context.dispatcher

  //@Make time span configurable
  val checkForRunfolder =
    context.system.scheduler.schedule(0.seconds, 30.seconds, self, {
      val results = IlluminaProcessingUnit.checkForReadyProcessingUnits(
        new File(runfolderRootPath),
        new File(samplesheetPath),
        new File(qcControlConfigPath),
        new File(defaultQCConfigFile),
        new File(programConfigPath),
        new File(defaultProgramConfigFile),
        log)

      for (result <- results) {
        HerculesMainProtocol.FoundProcessingUnitMessage(result)
      }

    })

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = checkForRunfolder.cancel()

  // Just pass the message on to the parent (the IlluminaProcessingUnitWatcherActor)
  def receive = {
    case message: HerculesMainProtocol.FoundProcessingUnitMessage =>
      context.parent ! message
  }

}