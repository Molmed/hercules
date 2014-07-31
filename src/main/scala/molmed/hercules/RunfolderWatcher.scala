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
      log.info("Start watching")

      val master = sender

      import context._

      // Search for runfolders
      system.scheduler.schedule(0.seconds, 5.seconds) {

        val results =
          RunfolderWatcher.findRunfoldersToProcess(log, runfolderRoots, sampleSheetRoot)

        for (result <- results) {
          log.info("Sending result to Master: " + result)
          master ! ProcessRunFolderMessage(result)
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

    def listSubDirectories(dir: File): Seq[File] = {
      require(dir.isDirectory(), dir + " was not a directory!")
      dir.listFiles().filter(p => p.isDirectory())
    }

    def searchForRunfolders(): Seq[File] = {

      def modifiedMoreThanOneHourAgo(file: File): Boolean = {
        val currentTime = new Date().getTime()
        val modifiedTime = new Date(file.lastModified()).getTime()
        currentTime - modifiedTime > 1000 * 60 * 60
      }

      def filterOutReadyForProcessing(runfolders: Seq[File]): Seq[File] =
        {
          runfolders.filter(runfolder => {
            val filesInRunFolder = runfolder.listFiles()
            filesInRunFolder.forall(file => {
              !(file.getName() == ".found") && modifiedMoreThanOneHourAgo(file)
            })
          })
        }

      runfolderRoots.flatMap(runfolder =>
        filterOutReadyForProcessing(
          listSubDirectories(runfolder)))
    }

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