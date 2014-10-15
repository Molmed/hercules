package hercules.actors.demultiplexing

import akka.actor.Props
import akka.event.LoggingReceive
import akka.pattern.pipe
import hercules.actors.HerculesActor
import hercules.protocols.HerculesMainProtocol._
import hercules.external.program.Sisyphus
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random
import java.io.File
import hercules.demultiplexing.Demultiplexer
import hercules.demultiplexing.DemultiplexingResult
import hercules.exceptions.HerculesExceptions

object SisyphusDemultiplexingExecutorActor {

  def props(demultiplexer: Demultiplexer = new Sisyphus()): Props =
    Props(
      new SisyphusDemultiplexingExecutorActor(
        demultiplexer,
        60.seconds))

}

/**
 * Concrete executor implementation for demultiplexing using Sisyphus
 * This one can lock while doing it work.
 */
class SisyphusDemultiplexingExecutorActor(demultiplexer: Demultiplexer, requestWorkInterval: FiniteDuration) extends DemultiplexingActor {

  import context.dispatcher

  //@TODO Make request new work period configurable.
  val requestWork =
    context.system.scheduler.schedule(
      (requestWorkInterval +
        Random.nextInt(requestWorkInterval.toSeconds.toInt).seconds),
      requestWorkInterval,
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
        notice.info(s"Starting to demultiplex: $unit!")
        // Acknowledge to sender that we will process this and become busy
        sender ! Acknowledge
        context.become(busyWorker)

        // Run the demultiplexing and pipe the result, when available, to self as a message 
        // @TODO Handle exception
        demultiplexer.demultiplex(unit).map(
          (r: DemultiplexingResult) =>
            if (r.success) {
              log.info("Successfully demultiplexed: " + r.unit)
              notice.info("Demultiplexing finished for processingunit: " + r.unit)
              FinishedDemultiplexingProcessingUnitMessage(r.unit)
            } else {
              log.info("Failed in demultiplexing: " + r.unit)
              notice.critical(s"Failed demultiplexing for: $r.unit with the reason: $r.logText")
              FailedDemultiplexingProcessingUnitMessage(r.unit, r.logText.getOrElse("Unknown reason"))
            }
        ).recover {
            case e: HerculesExceptions.ExternalProgramException =>
              FailedDemultiplexingProcessingUnitMessage(e.unit, e.message)
          }.pipeTo(self)
      } else {
        sender ! Reject(Some(s"The run folder path $pathToTheRunfolder could not be found"))
      }
    }
  }
}
