package molmed.hercules

import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import akka.event.Logging
import scala.concurrent.duration._
import java.io.File
import collection.JavaConversions._
import molmed.hercules.messages._
import molmed.hercules.processes.ListDirectoryProcess
import molmed.hercules.processes.biotank.WriteTestFileToDirectoryProcess
import molmed.hercules.processes.biotank.DemultiplexingProcess

class RunfolderProcessor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case ProcessRunFolderMessage(runfolder) => {

      val master = sender

      log.info("Received: ProcessRunFolderMessage")

      runfolder.state match {
        case ProcessingState.RunningDemultiplexing => {
          log.info("Starting demultiplexing on: " +
            runfolder.runfolder.getName())
          val demultiplexedRunfolder =
            RunfolderProcessor.runDemultiplexing(log, runfolder)
          master ! ProcessRunFolderMessage(demultiplexedRunfolder)
        }
      }

    }
  }
}

object RunfolderProcessor {

  def runDemultiplexing(
    log: akka.event.LoggingAdapter,
    runfolder: Runfolder): Runfolder = {

    val demultiplex = new DemultiplexingProcess(runfolder)

    try {
      demultiplex.start()
      val finishedRunfolder =
        new Runfolder(runfolder.runfolder, runfolder.samplesheet, ProcessingState.FinishedDemultiplexing)
      log.info("Succesfully demultiplexed " + runfolder.runfolder.getName())
      finishedRunfolder
    } catch {
      case e: Throwable => {
        log.error("Error while demultiplexing: " + runfolder.runfolder.getName())
        new Runfolder(
          runfolder.runfolder,
          runfolder.samplesheet,
          ProcessingState.Halted)
      }
    }

  }

}