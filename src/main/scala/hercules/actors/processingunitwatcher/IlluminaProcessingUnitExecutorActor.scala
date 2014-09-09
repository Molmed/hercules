package hercules.actors.processingunitwatcher

import hercules.actors.HerculesActor
import hercules.entities.ProcessingUnit
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import java.io.File
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.config.processingunit.ProcessingUnitConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig

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

  /**
   * Indicate if the unit is ready to be processed.
   * Normally this involves checking files on the file system or reading it's
   * status from a database.
   *
   * @param unit The processing unit check
   * @return if the processing unit is ready to be processed or not.
   */
  private def isReadyForProcessing(unit: ProcessingUnit): Boolean = {

    val runfolderPath = new File(unit.uri)

    val filesInRunFolder = runfolderPath.listFiles()

    val hasNoFoundFile =
      filesInRunFolder.forall(file => {
        !(file.getName() == "found")
      })

    val hasRTAComplete =
      filesInRunFolder.exists(x => x.getName() == "RTAComplete.txt")

    hasNoFoundFile && hasRTAComplete
  }

  /**
   * Checks runfolders (IlluminaProcessingUnits) which are ready to be processed
   * @param runfolderRoot
   * @param sampleSheetRoot
   * @param customQCConfigRoot
   * @param defaultQCConfigFile
   * @param customProgramConfigRoot
   * @param defaultProgramConfigFile
   * @param log
   * @return A sequence of Illumina processingUnit which are ready to be
   * processed
   *
   */
  def checkReadyForRunfolders(
    runfolderRoot: File,
    sampleSheetRoot: File,
    customQCConfigRoot: File,
    defaultQCConfigFile: File,
    customProgramConfigRoot: File,
    defaultProgramConfigFile: File,
    log: akka.event.LoggingAdapter): Seq[IlluminaProcessingUnit] = {

    /**
     * List all of the subdirectories of dir.
     */
    def listSubDirectories(dir: File): Seq[File] = {
      require(dir.isDirectory(), dir + " was not a directory!")
      dir.listFiles().filter(p => p.isDirectory())
    }

    /**
     * Search from the specified runfolder roots for runfolders which are
     * ready to be processed.
     */
    def searchForRunfolders(): Seq[File] = {
      // Make sure to only get folders which begin with a date (or six digits
      // to be precise)
      listSubDirectories(runfolderRoot).filter(p =>
        p.getName.matches("""^\d{6}.*$"""))
    }

    /**
     * Search for a samplesheet matching the found runfolder.
     */
    def searchForSamplesheet(runfolder: File): Option[File] = {
      val runfolderName = runfolder.getName()

      val samplesheet = sampleSheetRoot.listFiles().
        find(p => p.getName() == runfolderName + "_samplesheet.csv")

      if (samplesheet.isDefined)
        log.info("Found matching samplesheet for: " + runfolder.getName())
      else
        log.info("Did not find matching samplesheet for: " + runfolder.getName())

      samplesheet
    }

    /**
     * Add a hidden .found file, in the runfolder.
     */
    def markAsFound(runfolder: File): Boolean = {
      log.info("Marking: " + runfolder.getName() + " as found.")
      new File(runfolder + "/found").createNewFile()
    }

    /**
     * Gets a special qc config if there is one. If there is not returns the
     * default one based on the type of run.
     *
     * Right now we use Sisyphus and than always wants the same file,
     * so there is really only on type of default file to get.
     *
     * @param runfolder The runfolder to get the quality control definition file for
     * @return the QC control config file or None
     */
    def getQCConfig(runfolder: File): Option[File] = {
      val customFile =
        customQCConfigRoot.listFiles().
          find(qcFile =>
            qcFile.getName().startsWith(runfolder.getName() + "_qc.xml"))

      if (customFile.isDefined)
        customFile
      else {
        Some(defaultQCConfigFile)
      }
    }

    /**
     * Gets a special program config if there is one. If there is not returns the
     * default one based on the type of run.
     *
     * Right now we use Sisyphus and than always wants the same file,
     * so there is really only on type of default file to get.
     *
     * @param runfolder The runfolder to get the quality control definition file for
     * @return the program control config file or None
     */
    def getProgramConfig(runfolder: File): Option[File] = {
      val customFile =
        customProgramConfigRoot.listFiles().
          find(programFile =>
            programFile.getName().startsWith(runfolder.getName() + "_sisyphus.yml"))

      if (customFile.isDefined)
        customFile
      else {
        Some(defaultProgramConfigFile)
      }
    }

    for {
      runfolder <- searchForRunfolders()
      samplesheet <- searchForSamplesheet(runfolder)
      qcConfig <- getQCConfig(runfolder)
      programConfig <- getProgramConfig(runfolder)
      illuminaProcessingUnit <- {
        val unitConfig =
          new IlluminaProcessingUnitConfig(samplesheet, qcConfig, Some(programConfig))
        Some(new IlluminaProcessingUnit(unitConfig, runfolder.toURI()))
      }
      if isReadyForProcessing(illuminaProcessingUnit)
    } yield {
      illuminaProcessingUnit
    }
  }

}

class IlluminaProcessingUnitExecutorActor(
    val runfolderRootPath: String,
    val samplesheetPath: String,
    val qcControlConfigPath: String,
    val defaultQCConfigFile: String,
    val programConfigPath: String,
    val defaultProgramConfigFile: String) extends HerculesActor with ProcessingUnitWatcherActor {

//  val runfolders = IlluminaProcessingUnitExecutorActor.
//    checkReadyForRunfolders(
//      new File(runfolderRootPath),
//      new File(samplesheetPath),
//      new File(qcControlConfigPath),
//      new File(defaultQCConfigFile),
//      new File(programConfigPath),
//      new File(defaultProgramConfigFile),
//      log)

  def receive = ???

}