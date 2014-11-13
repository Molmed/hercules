package hercules.actors.demultiplexing

import akka.actor.Props
import akka.event.LoggingReceive
import akka.pattern.pipe
import hercules.actors.HerculesActor
import hercules.demultiplexing.Demultiplexer
import hercules.demultiplexing.DemultiplexingResult
import hercules.exceptions.HerculesExceptions
import hercules.external.program.Sisyphus
import hercules.protocols.HerculesMainProtocol._
import java.io.File
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random
import akka.util.Timeout

/**
 * Provided factor methods for the SisyphusDemultiplexingExecutorActor
 */
object SisyphusDemultiplexingExecutorActor {

  /**
   * Provides props for creating a SisyphusDemultiplexingExectorActor
   * @param demultiplexer Will default to Sisyphus
   * @return Props to create a SisyphusDemultiplexingExectorActor
   */
  def props(demultiplexer: Demultiplexer = new Sisyphus()): Props =
    Props(
      new SisyphusDemultiplexingExecutorActor(
        demultiplexer))
}

/**
 * Concrete executor implementation for demultiplexing using Sisyphus
 * This one can lock while doing it work.
 * @param demultiplexer The actual demultiplexer to user.
 */
class SisyphusDemultiplexingExecutorActor(demultiplexer: Demultiplexer) extends DemultiplexingActor {

  def receive = LoggingReceive {

    case StartDemultiplexingProcessingUnitMessage(unit) => {

      val originalSender = sender

      //@TODO It is probably reasonable to have some other mechanism than checking if it
      // can spot the file if it can spot the file or not. But for now, this will have to do.
      val pathToTheRunfolder = new File(unit.uri)
      // Check if we are able to process the unit
      if (pathToTheRunfolder.exists()) {

        log.info(s"Starting to demultiplex: $unit!")
        notice.info(s"Starting to demultiplex: $unit!")

        // Acknowledge to sender that we will process this
        sender ! Acknowledge

        //@TODO Not 100% sure that it's good to do it this way, but we'll try it for now.
        import context.dispatcher

        // Run the demultiplexing and pipe the result, when available, to self as a message 
        demultiplexer.demultiplex(unit).map(
          (r: DemultiplexingResult) =>
            if (r.success) FinishedDemultiplexingProcessingUnitMessage(r.unit)
            else FailedDemultiplexingProcessingUnitMessage(r.unit, r.logText.getOrElse("Unknown reason"))).recover {
            case e: HerculesExceptions.ExternalProgramException =>
              FailedDemultiplexingProcessingUnitMessage(e.unit, e.message)
          }.pipeTo(originalSender)

      } else {
        sender ! Reject(Some(s"The run folder path $pathToTheRunfolder could not be found"))
      }
    }

  }
}

