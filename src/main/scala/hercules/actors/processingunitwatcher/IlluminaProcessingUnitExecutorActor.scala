package hercules.actors.processingunitwatcher

import hercules.actors.HerculesActor
import hercules.entities.ProcessingUnit
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import java.io.File
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.config.processingunit.ProcessingUnitConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.entities.illumina.MiSeqProcessingUnit
import hercules.entities.illumina.HiSeqProcessingUnit

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

  //@TODO to pick up the processing units, us the function.
  // It will delagate and pick up the correct sub type.
  //  val result = IlluminaProcessingUnit.checkForReadyProcessingUnits(
  //    new File(runfolderRootPath),
  //    new File(samplesheetPath),
  //    new File(qcControlConfigPath),
  //    new File(defaultQCConfigFile),
  //    new File(programConfigPath),
  //    new File(defaultProgramConfigFile),
  //    log)

  def receive = ???

}