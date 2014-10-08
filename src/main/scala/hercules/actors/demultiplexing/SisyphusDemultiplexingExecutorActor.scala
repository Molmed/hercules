package hercules.actors.demultiplexing

import akka.actor.Props
import hercules.actors.HerculesActor
import hercules.protocols.HerculesMainProtocol._
import hercules.external.program.Sisyphus
import scala.io.Source
import scala.concurrent._
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

  def receive = {
    case StartDemultiplexingProcessingUnitMessage(unit) => {

      log.info(s"Starting to demultiplex: $unit!")

      // Run the demultiplexing as a blocking operation for now
      // @TODO set a (configurable) maximum duration for the demultiplexing call?
      val DemultiplexingResult(success, logFile): DemultiplexingResult =
        Await.result[DemultiplexingResult](
          demultiplexer.demultiplex(unit),
          duration.Duration.Inf)

      if (success) {
        log.info("Successfully demultiplexed: " + unit)
        sender ! FinishedDemultiplexingProcessingUnitMessage(unit)
      } else {
        log.info("Failed in demultiplexing: " + unit)
        demultiplexer.cleanup(unit)
        val logText =
          if (logFile.isDefined)
            Source.fromFile(logFile.get).getLines.mkString
          else
            ""
        sender ! FailedDemultiplexingProcessingUnitMessage(unit, logText)
      }

    }

  }

}