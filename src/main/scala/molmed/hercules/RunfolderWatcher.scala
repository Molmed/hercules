package molmed.hercules

import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import akka.event.Logging
import scala.concurrent.duration._
import java.io.File
import collection.JavaConversions._
import java.util.Date
import molmed.hercules.messages._

//@TODO Fix the default paths!
class RunfolderWatcher(
  runfolderRoots: Seq[File],
  sampleSheetRoot: File)
    extends Actor {

  val log = Logging(context.system, this)

  log.info("RunfolderWatcher starting")

  def receive = {
    case StartMessage() => {
      log.info("Start watching for new runfolders under: ")
      runfolderRoots.foreach(x => log.info("    " + x))

      val master = sender

      import context._

      // Search for runfolders
      // @TODO Make time between the searches configurable
      system.scheduler.schedule(0.seconds, 5.seconds) {

        val runfolders =
          RunfolderWatcher.findRunfoldersToProcess(log, runfolderRoots, sampleSheetRoot)

        for (runfolder <- runfolders) {
          log.info("Sending runfolder to Master: " + runfolder)
          master ! ProcessRunFolderMessage(runfolder)
        }
      }
    }
  }
}

object RunfolderWatcher {

  def findRunfoldersToProcess(
    log: akka.event.LoggingAdapter,
    runfolderRoots: Seq[File],
    sampleSheetRoot: File): Seq[Runfolder] = {

    /**
     * Lots of helper functions
     */

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
       * Filter method to only get files which are processed more than one
       * hour ago.
       * @TODO Make the time from modification a setting.
       */
      def modifiedMoreThanOneHourAgo(file: File): Boolean = {
        val currentTime = new Date().getTime()
        val modifiedTime = new Date(file.lastModified()).getTime()
        currentTime - modifiedTime > 1000 // * 60 * 60
      }

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
                !(file.getName() == ".found") &&
                  modifiedMoreThanOneHourAgo(file)
              })
            val hasRTAComplete =
              filesInRunFolder.exists(x => x.getName() == "RTAComplete.txt")
            criterias && hasRTAComplete
          })
        }

      runfolderRoots.flatMap(runfolder =>
        filterOutReadyForProcessing(
          listSubDirectories(runfolder)))
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
      new File(runfolder + "/.found").createNewFile()
    }

    /**
     * Run the actual method!
     */
    for {
      runfolder <- searchForRunfolders()
      samplesheet <- searchForSamplesheet(runfolder)
    } yield {
      markAsFound(runfolder)
      new Runfolder(runfolder, samplesheet, ProcessingState.Found)
    }

  }

}