package molmed.hercules

import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import akka.event.Logging
import scala.concurrent.duration._
import java.io.File
import ProcessingState._
import molmed.hercules.messages._

class Master() extends Actor with akka.actor.ActorLogging {

  //@TODO Make configurable
  val runfolders = Seq(
    new File("/seqdata/biotank1/runfolders/"),
    new File("/seqdata/biotank2/runfolders/"))
  val samplesheets = new File("/srv/samplesheet/Processning/")

  log.info("Master starting")

  val runfolderWatcher = context.actorOf(
    Props(new RunfolderWatcher(runfolders, samplesheets)),
    "RunfolderWatcher")

  val runfolderProcessor = context.actorOf(
    Props(new RunfolderProcessor()),
    "RunfolderProcessor")

  runfolderWatcher ! StartMessage()

  def receive = {
    case message: ProcessRunFolderMessage => {

      log.info("Got a ProcessRunFolderMessage")

      message.runfolder.state match {
        case ProcessingState.Found => {
          log.info("Found it, let's demultiplex it!")
          val runfolderToBeDemultiplexed =
            new Runfolder(
              message.runfolder.runfolder,
              message.runfolder.samplesheet,
              ProcessingState.RunningDemultiplexing)
          runfolderProcessor ! new ProcessRunFolderMessage(runfolderToBeDemultiplexed)
        }

        case ProcessingState.FinishedDemultiplexing =>
          log.info("Demultiplexing and upload to uppmax is finished!")
        //@TODO Here aeacus report should be started on Uppmax!
        case ProcessingState.Finished =>
          log.info("And it's finished!")
          self ! StopMessage
      }

    }
    case StopMessage => {
      context.system.shutdown()
    }
  }

}