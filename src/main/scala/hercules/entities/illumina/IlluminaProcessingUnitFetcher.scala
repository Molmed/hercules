package hercules.entities.illumina

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import com.typesafe.config.ConfigFactory
import hercules.utils.Constants
import scala.Option.option2Iterable
import akka.event.LoggingAdapter
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.entities.ProcessingUnit
import java.net.InetAddress
import hercules.entities.ProcessingUnitFetcher
import hercules.config.processingunit.ProcessingUnitFetcherConfig
import hercules.config.processingunit.IlluminaProcessingUnitFetcherConfig

/**
 * Provided factory methods for IlluminaProcessingUnitFetcher
 */
object IlluminaProcessingUnitFetcher {

  /**
   * Create a new IlluminaProcessingUnitFetcher
   * @return a IlluminaProcessingUnitFetcher
   */
  def apply(): IlluminaProcessingUnitFetcher = new IlluminaProcessingUnitFetcher()

}

/**
 * IlluminaProcessingUnitFetcher will look for Illumina processing units by
 * scanning the the file system in the places indicated in the configuration.
 */
class IlluminaProcessingUnitFetcher() extends ProcessingUnitFetcher {

  type FetherConfigType = IlluminaProcessingUnitFetcherConfig
  type ProcessingUnitType = IlluminaProcessingUnit

  /**
   * Checks is a runfolder is ready for processing based on the
   * path to the runfolder.
   * @param runfolder path to the runfolder
   * @return true if the runfolder should be processed.
   */
  def isReadyForProcessing(runfolder: File): Boolean = {

    val filesInRunFolder = runfolder.listFiles()

    val hasIndicatorFile =
      filesInRunFolder.exists(x => x.getName() == IlluminaProcessingUnit.nameOfIndicatorFile)

    val hasRTAComplete =
      filesInRunFolder.exists(x => x.getName() == "RTAComplete.txt")

    !hasIndicatorFile && hasRTAComplete
  }

  /**
   * Indicate if the unit is ready to be processed.
   * Normally this involves checking files on the file system or reading it's
   * status from a database.
   *
   * @param unit The processing unit check
   * @return if the processing unit is ready to be processed or not.
   */
  def isReadyForProcessing(unit: IlluminaProcessingUnit): Boolean = {
    val runfolderPath = new File(unit.uri)
    isReadyForProcessing(runfolderPath)
  }

  /**
   *  Searches for runfolders corresponding to the supplied ProcessingUnit name and,
   *  if found, returns the appropriate IlluminaProcessingUnit.
   *  @param unitName The name of a ProcessingUnit to search for
   *  @param config
   *  @return An Option[ProcessingUnit] containing the match or None if not found
   */
  def searchForProcessingUnitName(
    unitName: String,
    config: IlluminaProcessingUnitFetcherConfig): Option[IlluminaProcessingUnit] = {
    getProcessingUnits(config, filter = _ => true).find { _.name == unitName }
  }

  /**
   * Checks runfolders (IlluminaProcessingUnits) which are ready to be processed
   * It will delagate and return the correct sub type (MiSeq, or HiSeq) processing unit.
   * @param config
   * @return A sequence of Illumina processingUnit which are ready to be
   * processed
   */
  def checkForReadyProcessingUnits(
    config: IlluminaProcessingUnitFetcherConfig): Seq[IlluminaProcessingUnit] = {

    for {
      unit <- getProcessingUnits(config)
    } yield {
      unit.markAsFound
      unit
    }
  }

  /**
   *  Get all available ProcessingUnits
   *
   *  @param config
   *  @param filter to apply to the processing - will by default exclude any
   *                runfolders already found.
   *  @return All IlluminaProcessingUnits
   */
  private def getProcessingUnits(
    config: IlluminaProcessingUnitFetcherConfig,
    filter: File => Boolean = isReadyForProcessing): Seq[IlluminaProcessingUnit] = {

    val log = config.log

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
      config.runfolderRoots.flatMap(dir => listSubDirectories(dir).filter(p =>
        p.getName.matches("""^\d{6}.*$""")))
    }

    /**
     * Will search the `rootFolder` for files matching first the
     * runfolder name, and then the flowcell name associated with that
     * runfolder.
     * @param rootFolder to search in
     * @param runfolder to use for matching
     * @param fileSuffix the suffix of the file to search for, e.g. "_samplesheet.csv"
     * @return
     */
    def findFileFromRunfolderOrFlowcell(rootFolder: File, runfolder: File, fileSuffix: String): Option[File] = {

      val potentialFiles = rootFolder.listFiles()
      val runfolderName = runfolder.getName()

      def findFileFromFlowcellName(): Option[File] = {
        val flowcellName = runfolder2flowcell(runfolder)
        potentialFiles.
          find(p => p.getName() == flowcellName + fileSuffix)
      }

      def findFileFromRunfolderName(): Option[File] = {
        potentialFiles.
          find(p => p.getName() == runfolderName + fileSuffix)
      }

      findFileFromRunfolderName().orElse(findFileFromFlowcellName())

    }

    /**
     * Convert a runfolder to it's flowcell name, based on
     * the directory name of the runfolder.
     * @param runfolder
     * @return the name of the flowcell of the runfolder.
     */
    def runfolder2flowcell(runfolder: File): String = {
      // The runfolder contains the name of the flowcell,
      // but for HiSeq and HiSeqX it also contains the position on the
      // instrument, i.e. 'A' or 'B', this needs to be dropped.
      val flowcellAndPossiblePosition = runfolder.getName().split("_")(3)

      val flowcellName = getMachineTypeFromRunParametersXML(runfolder) match {
        case Constants.MISEQ_CONTROL_SOFTWARE => flowcellAndPossiblePosition
        case _                                => flowcellAndPossiblePosition.drop(1)
      }

      flowcellName
    }

    /**
     * Search for a samplesheet matching the found runfolder.
     */
    def searchForSamplesheet(runfolder: File): Option[File] = {

      require(
        config.sampleSheetRoot.exists(),
        s"${config.sampleSheetRoot} does not exist! Create it or provide a valid value in application.conf")

      val samplesheet: Option[File] =
        findFileFromRunfolderOrFlowcell(config.sampleSheetRoot, runfolder, "_samplesheet.csv")

      if (samplesheet.isDefined)
        log.debug("Found matching samplesheet for: " + runfolder.getName())
      else
        log.debug("Did not find matching samplesheet for: " +
          runfolder.getName() + " in : " + config.sampleSheetRoot)

      samplesheet
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

      require(config.customQCConfigRoot.exists() && config.customQCConfigRoot.isDirectory(),
        config.customQCConfigRoot.getAbsolutePath() + " does not exist!")

      val customFile = findFileFromRunfolderOrFlowcell(config.customQCConfigRoot, runfolder, "_qc.xml")

      if (customFile.isDefined) {
        log.debug("Found custom qc config file for: " + runfolder.getName())
        customFile
      } else {
        log.debug("Using default qc config file for: " + runfolder.getName())

        if (config.defaultQCConfigFile.exists())
          Some(config.defaultQCConfigFile)
        else
          throw new FileNotFoundException(
            "Didn't find default qc config file: " +
              config.defaultQCConfigFile.getAbsolutePath())
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

      require(config.customProgramConfigRoot.exists() && config.customProgramConfigRoot.isDirectory(),
        config.customProgramConfigRoot.getAbsolutePath() + " does not exist!")

      val customFile = findFileFromRunfolderOrFlowcell(config.customProgramConfigRoot, runfolder, "_sisyphus.yml")

      if (customFile.isDefined) {
        log.debug("Found custom program config file for: " + runfolder.getName())
        customFile
      } else {
        log.debug("Using default program config file for: " + runfolder.getName())

        if (config.defaultProgramConfigFile.exists())
          Some(config.defaultProgramConfigFile)
        else
          throw new FileNotFoundException(
            "Didn't find default program config file: " +
              config.defaultProgramConfigFile.getAbsolutePath())
      }
    }

    /**
     * Extract the [R|r]unParameters.xml file and return it as a xml.Elem
     * that can be queried.
     * @param runfolder the runfolder to look in.
     * @return the runParameters.xml file as a xml.Elem
     */
    def getRunparametersXML(runfolder: File): scala.xml.Elem = {

      val configLoader = ConfigFactory.load()
      val runParameters = configLoader.getString("general.runParameters")

      val runInfoXML = runfolder.listFiles().
        find(x => x.getName().equalsIgnoreCase(runParameters)).
        getOrElse(throw new FileNotFoundException(
          "Did not find " + runParameters + " .xml in runfolder: " +
            runfolder.getAbsolutePath()))

      scala.xml.XML.loadFile(runInfoXML)
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

      val xml = getRunparametersXML(runfolder)
      val applicationName = xml \\ "ApplicationName"

      assert(applicationName.length == 1,
        """Didn't find exactly one instance of "ApplicationName" in """ +
          """RunParameters.xml in runfolder: """ + runfolder.getName())

      applicationName.text
    }

    /**
     * Fetch the manifest file names, if they are specified, from
     * runParameters.xml.
     * @param runfolder
     * @return Seq[String] with file names
     */
    def getManifestFilesFromRunParametersXML(runfolder: File): Seq[String] = {
      val xml = getRunparametersXML(runfolder)
      val manifestFiles = xml \\ "ManifestFiles"
      val files: Seq[String] = for (file <- xml \\ "ManifestFiles" \\ "string") yield file.text.toString
      files
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

      getMachineTypeFromRunParametersXML(runfolder) match {
        case Constants.MISEQ_CONTROL_SOFTWARE => Some(new MiSeqProcessingUnit(unitConfig, runfolder.toURI(),
          getManifestFilesFromRunParametersXML(runfolder).size > 0))
        case Constants.HISEQ_CONTROL_SOFTWARE   => Some(new HiSeqProcessingUnit(unitConfig, runfolder.toURI()))
        case Constants.HISEQ_X_CONTROL_SOFTWARE => Some(new HiSeqProcessingUnit(unitConfig, runfolder.toURI()))
        case s: String                          => throw new Exception(s"Unrecognized type string:  $s")
      }

    }

    for {
      runfolder <- searchForRunfolders()
      if filter(runfolder)
      samplesheet <- searchForSamplesheet(runfolder)
      qcConfig <- getQCConfig(runfolder)
      programConfig <- getProgramConfig(runfolder)
      illuminaProcessingUnit <- constructCorrectProcessingUnitType(runfolder, samplesheet, qcConfig, programConfig)
    } yield {
      illuminaProcessingUnit
    }
  }

}
