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

  def props(): Props = {

    val conf = ConfigFactory.load("IlluminaProcessingUnitExecutorActor")
    val runfolderPath = conf.getString("runFolderPath")
    val samplesheetPath = conf.getString("samplesheetPath")
    val customQCConfigurationRoot = conf.getString("customQCConfigurationFilesRoot")
    val defaultQCConfigFile = conf.getString("defaultQCConfigFile")

    Props(new IlluminaProcessingUnitExecutorActor(
      runfolderPath,
      samplesheetPath,
      customQCConfigurationRoot,
      defaultQCConfigFile))
  }

  /**
   * Indicate if the unit is ready to be processed.
   * Normally this involves checking files on the file system or reading it's
   * status from a database.
   *
   * @param unit The processing unit check
   * @return if the processing unit is ready to be processed or not.
   */
  private def isReadyForProcessing(unit: ProcessingUnit): Boolean = ???

  /**
   * @TODO Write documentation here!
   */
  def checkReadyForRunfolders(
    root: File,
    sampleSheetRoot: File,
    customQCConfigRoot: File,
    defaultConfigFile: File,
    log: akka.event.LoggingAdapter): Seq[IlluminaProcessingUnit] = {
    /**
     * List all of the subdirectories of dir.
     * @TODO Might want to make sure that this only locks at folders which
     * match the runfolder pattern.
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

      /**
       * Find the runfolders which are ready for processing.
       * That is, the do not have a found file in them (they are already being
       *  processed), or that have been modified less than a time x ago, and
       *  finally the RTAComplete.txt nees to be in place.
       */
      def filterOutReadyForProcessing(runfolders: Seq[File]): Seq[File] =
        {
          runfolders.filter(runfolder => {
            val filesInRunFolder = runfolder.listFiles()

            val criterias =
              filesInRunFolder.forall(file => {
                !(file.getName() == "found")
              })
            val hasRTAComplete =
              filesInRunFolder.exists(x => x.getName() == "RTAComplete.txt")
            criterias && hasRTAComplete
          })
        }

      // Make sure to only get folders which begin with a date (or six digits
      // to be precise)
      filterOutReadyForProcessing(
        listSubDirectories(root).filter(p =>
          p.getName.matches("""^\d{6}.*$""")))
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
     * @return the QC control config file.
     */
    def getQCConfig(runfolder: File): File = {
      customQCConfigRoot.listFiles().
        find(qcFile =>
          qcFile.getName().startsWith(runfolder.getName() + "_qc.xml")).
        getOrElse(defaultConfigFile)

    }

    val allRunfolders = searchForRunfolders()

    //@TODO If a runfolder does not have a matching sample sheet
    // we will want a notification about that!
    val onlyRunfoldersWithSampleSheets = allRunfolders.filter(
      runfolder => searchForSamplesheet(runfolder).isDefined)

    onlyRunfoldersWithSampleSheets.
      map(runfolder => {
        val sampleSheet = searchForSamplesheet(runfolder).get
        val qcConfig: File = getQCConfig(runfolder)
        //@TODO Get program config here!
        val programConfig: Option[File] = ???
        val unitConfig = new IlluminaProcessingUnitConfig(sampleSheet, qcConfig, programConfig)
        new IlluminaProcessingUnit(unitConfig, runfolder.toURI())
      })

  }

}

class IlluminaProcessingUnitExecutorActor(
    runfolderRootPath: String,
    samplesheetPath: String,
    qcControlConfigPath: String,
    defaultQCConfigFile: String) extends HerculesActor with ProcessingUnitWatcherActor {

  val runfolders = IlluminaProcessingUnitExecutorActor.
    checkReadyForRunfolders(
      new File(runfolderRootPath),
      new File(samplesheetPath),
      new File(qcControlConfigPath),
      new File(defaultQCConfigFile),
      log)

  def receive = ???

}