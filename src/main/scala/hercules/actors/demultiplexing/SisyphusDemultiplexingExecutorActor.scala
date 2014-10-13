package hercules.actors.demultiplexing

import akka.actor.Props
import akka.event.LoggingReceive
import akka.pattern.pipe
import hercules.actors.HerculesActor
import hercules.protocols.HerculesMainProtocol._
import hercules.external.program.Sisyphus
import scala.concurrent.{ duration, Future }
import java.io.File
import hercules.demultiplexing.Demultiplexer
import hercules.demultiplexing.DemultiplexingResult

object SisyphusDemultiplexingExecutorActor {

  def props(demultiplexer: Demultiplexer = new Sisyphus()): Props =
    Props(new SisyphusDemultiplexingExecutorActor(demultiplexer))

}

/**
 * Concrete executor implementation for demultiplexing using Sisyphus
 * This one can lock while doing it work.
 */
class SisyphusDemultiplexingExecutorActor(demultiplexer: Demultiplexer) extends DemultiplexingActor {

  import context.dispatcher
  import duration._

  //@TODO Make request new work period configurable.
  // Request new work periodically
  val requestWork =
    context.system.scheduler.schedule(
      30.seconds,
      10.seconds,
      self,
      { RequestDemultiplexingProcessingUnitMessage })

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    requestWork.cancel()
  }

  def receive = idleWorker

  // Behavior when busy
  def busyWorker: Receive = LoggingReceive {

    case RequestExecutorAvailabilityMessage =>
      sender ! Busy

    // Reject any incoming demultiplex requests while we are busy
    case StartDemultiplexingProcessingUnitMessage(unit) =>
      sender ! Reject

    // Incoming demultiplexing results are passed up to parent and we become idle
    case message @ (_: FinishedDemultiplexingProcessingUnitMessage | _: FailedDemultiplexingProcessingUnitMessage) => {
      context.parent ! message
      context.become(idleWorker)

      message match {
        case msg: FinishedDemultiplexingProcessingUnitMessage =>
          log.info("Successfully demultiplexed: " + msg.unit)
        case msg: FailedDemultiplexingProcessingUnitMessage =>
          log.info("Failed in demultiplexing: " + msg.unit)
      }
    }
  }

  // Behavior when idle
  def idleWorker: Receive = LoggingReceive {

    case RequestExecutorAvailabilityMessage =>
      sender ! Idle

    // Pass a request for work up to parent
    case RequestDemultiplexingProcessingUnitMessage =>
      context.parent ! RequestDemultiplexingProcessingUnitMessage

    case StartDemultiplexingProcessingUnitMessage(unit) => {

      //@TODO It is probably reasonable to have some other mechanism than checking if it
      // can spot the file if it can spot the file or not. But for now, this will have to do.
      val pathToTheRunfolder = new File(unit.uri)
      // Check if we are able to process the unit
      if (pathToTheRunfolder.exists()) {

        log.info(s"Starting to demultiplex: $unit!")
        // Acknowledge to sender that we will process this and become busy
        sender ! Acknowledge
        context.become(busyWorker)

        // Run the demultiplexing and pipe the result, when available, to self as a message 
        // @TODO Handle exception
        demultiplexer.demultiplex(unit).map(
          (r: DemultiplexingResult) =>
            if (r.success) FinishedDemultiplexingProcessingUnitMessage(unit)
            else FailedDemultiplexingProcessingUnitMessage(unit, r.logText.getOrElse("Unknown reason"))
        ).pipeTo(self)
      } else {
        sender ! Reject
      }
    }
  }
}
