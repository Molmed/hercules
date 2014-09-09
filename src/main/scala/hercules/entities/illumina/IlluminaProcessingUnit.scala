package hercules.entities.illumina

import java.io.File
import java.net.URI
import hercules.entities.ProcessingUnit
import hercules.config.processingunit.ProcessingUnitConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig

object IlluminaProcessingUnit {
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
   * It will delagate and return the correct sub type (MiSeq, or HiSeq) processing unit.
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
  def checkForReadyProcessingUnits(
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
      (new File(runfolder + "/found")).createNewFile()
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

    /**
     * Based on the parameters found. Construct a MiSeq or a HiSeq processing
     * unit
     * @param runfolder
     * @param samplesheet
     * @param qcConfig
     * @param programConfig
     * @return A ProcessingUnit option
     */
    def constructCorrectProcessingUnitType(
      runfolder: File, 
      samplesheet: File, 
      qcConfig: File, 
      programConfig: File): Option[IlluminaProcessingUnit] = {
      val unitConfig =
        new IlluminaProcessingUnitConfig(samplesheet, qcConfig, Some(programConfig))

      //@TODO Some nicer solution for picking up if it's a HiSeq or MiSeq
      if (runfolder.getAbsolutePath().contains("MiSeq"))
        Some(new MiSeqProcessingUnit(unitConfig, runfolder.toURI()))
      else
        Some(new HiSeqProcessingUnit(unitConfig, runfolder.toURI()))
    }

    for {
      runfolder <- searchForRunfolders()
      samplesheet <- searchForSamplesheet(runfolder)
      qcConfig <- getQCConfig(runfolder)
      programConfig <- getProgramConfig(runfolder)
      illuminaProcessingUnit <- constructCorrectProcessingUnitType(runfolder, samplesheet, qcConfig, programConfig)
      if isReadyForProcessing(illuminaProcessingUnit)
    } yield {
      illuminaProcessingUnit
    }
  }
}

/**
 * Provides a base for representing a Illumina runfolder.
 */
abstract class IlluminaProcessingUnit(
    processingUnitConfig: ProcessingUnitConfig,
    uri: URI) extends ProcessingUnit {
}