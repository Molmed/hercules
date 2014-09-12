package hercules.entities.illumina

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import scala.Option.option2Iterable
import akka.event.LoggingAdapter
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.entities.ProcessingUnit
import java.net.InetAddress

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
    log: LoggingAdapter): Seq[IlluminaProcessingUnit] = {

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
        log.debug("Found matching samplesheet for: " + runfolder.getName())
      else
        log.debug("Did not find matching samplesheet for: " +
          runfolder.getName() + " in : " + sampleSheetRoot)

      samplesheet
    }

    /**
     * Add a found file, in the runfolder.
     */
    def markAsFound(runfolder: File): Boolean = {
      log.debug("Marking: " + runfolder.getName() + " as found.")
      (new File(runfolder + "/found")).createNewFile()
    }

    /**
     * Gets a special qc config if there is one. If there is not returns the
     * default one based on the type of run.
     *
     * Right now we use Sisyphus and than always wants the same file,
     * so there is really only on type of default file to get.
     *
     * It will throw a FileNotFoundException if the default file is missing.
     *
     * @param runfolder The runfolder to get the quality control definition file for
     * @return the QC control config file
     */
    def getQCConfig(runfolder: File): Option[File] = {

      assert(customQCConfigRoot.exists() && customQCConfigRoot.isDirectory(),
        customQCConfigRoot.getAbsolutePath() + " does not exist!")

      val customFile =
        customQCConfigRoot.listFiles().
          find(qcFile =>
            qcFile.getName().startsWith(runfolder.getName() + "_qc.xml"))

      if (customFile.isDefined) {
        log.debug("Found custom qc config file for: " + runfolder.getName())
        customFile
      } else {
        log.debug("Using default qc config file for: " + runfolder.getName())

        if (defaultQCConfigFile.exists())
          Some(defaultQCConfigFile)
        else
          throw new FileNotFoundException(
            "Didn't find default qc config file: " +
              defaultQCConfigFile.getAbsolutePath())
      }
    }

    /**
     * Gets a special program config if there is one. If there is not returns the
     * default one based on the type of run.
     *
     * Right now we use Sisyphus and than always wants the same file,
     * so there is really only on type of default file to get.
     *
     * It will throw a FileNotFoundException if the default file is missing.
     *
     * @param runfolder The runfolder to get the quality control definition file for
     * @return the program control config file or None
     */
    def getProgramConfig(runfolder: File): Option[File] = {

      assert(customProgramConfigRoot.exists() && customProgramConfigRoot.isDirectory(),
        customProgramConfigRoot.getAbsolutePath() + " does not exist!")

      val customFile =
        customProgramConfigRoot.listFiles().
          find(programFile =>
            programFile.getName().startsWith(runfolder.getName() + "_sisyphus.yml"))

      if (customFile.isDefined) {
        log.debug("Found custom program config file for: " + runfolder.getName())
        customFile
      } else {
        log.debug("Using default program config file for: " + runfolder.getName())

        if (defaultProgramConfigFile.exists())
          Some(defaultProgramConfigFile)
        else
          throw new FileNotFoundException(
            "Didn't find default qc config file: " +
              defaultQCConfigFile.getAbsolutePath())
      }
    }

    /**
     * Fetch the Application name from a runfolder from the RunParameters.xml
     * file. This should correspond to the instrument type used for Illumina
     * instruments
     * @param runfolder
     * @return The application name used, should only be: "HiSeq Control Software"
     * or "MiSeq Control Software" otherwise something has gone wrong.
     */
    def getMachineTypeFromRunParametersXML(runfolder: File): String = {
      val runInfoXML =
        runfolder.listFiles().
          find(x => x.getName() == "RunParameters.xml").
          getOrElse(throw new FileNotFoundException(
            "Did not find RunParameters.xml in runfolder: " +
              runfolder.getAbsolutePath()))

      val xml = scala.xml.XML.loadFile(runInfoXML)
      val applicationName = xml \\ "ApplicationName"

      assert(applicationName.length == 1,
        """Didn't find exactly one instance of "ApplicationName" in """ +
          """RunParameters.xml in runfolder: """ + runfolder.getName())

      applicationName.text
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

      import hercules.utils.Conversions.file2URI

      val unitConfig =
        new IlluminaProcessingUnitConfig(samplesheet, qcConfig, Some(programConfig))

      //@TODO Some nicer solution for picking up if it's a HiSeq or MiSeq
      getMachineTypeFromRunParametersXML(runfolder) match {
        case "MiSeq Control Software" => Some(new MiSeqProcessingUnit(unitConfig, runfolder.toURI()))
        case "HiSeq Control Software" => Some(new HiSeqProcessingUnit(unitConfig, runfolder.toURI()))
        case s: String                => throw new Exception(s"Unrecognized type string:  $s")
      }

    }

    for {
      runfolder <- searchForRunfolders()
      samplesheet <- searchForSamplesheet(runfolder)
      qcConfig <- getQCConfig(runfolder)
      programConfig <- getProgramConfig(runfolder)
      illuminaProcessingUnit <- constructCorrectProcessingUnitType(runfolder, samplesheet, qcConfig, programConfig)
      if isReadyForProcessing(illuminaProcessingUnit)
    } yield {
      markAsFound(runfolder)
      illuminaProcessingUnit
    }
  }
}

/**
 * Provides a base for representing a Illumina runfolder.
 */
abstract class IlluminaProcessingUnit() extends ProcessingUnit {

  val processingUnitConfig: IlluminaProcessingUnitConfig
  val uri: URI

  def name: String = new File(uri).getName()
}