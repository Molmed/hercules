package molmed.hercules

import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import akka.event.Logging
import scala.concurrent.duration._
import java.io.File
import collection.JavaConversions._
import molmed.hercules.messages._
import molmed.hercules.processes.ListDirectoryProcess
import molmed.hercules.processes.WriteTestFileToDirectoryProcess

class RunfolderProcessor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case ProcessRunFolderMessage(runfolder) => {

      val master = sender

      log.info("Received test: ProcessRunFolderMessage")

      val runningRunfolder =
        new Runfolder(
          runfolder.runfolder,
          runfolder.samplesheet,
          ProcessingState.Running)
      master ! ProcessRunFolderMessage(runningRunfolder)

      import context._

      WriteTestFileToDirectoryProcess(runningRunfolder).start()

      // @TODO 
      // This should be where the real async processing is done,
      // and then return the message saying that it's finished!
      system.scheduler.scheduleOnce(5.seconds) {
        val finishedRunfolder =
          new Runfolder(runfolder.runfolder, runfolder.samplesheet, ProcessingState.Finished)
        master ! ProcessRunFolderMessage(finishedRunfolder)
      }
    }
  }
}