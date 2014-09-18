package hercules.actors.demultiplexing

import akka.actor.Props
import hercules.actors.HerculesActor
import hercules.protocols.HerculesMainProtocol._
import hercules.external.program.Sisyphus
import scala.io.Source
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
class SisyphusDemultiplexingExecutorActor(demultiplexer: Demultiplexer) extends HerculesActor {

  def receive = {
    case StartDemultiplexingProcessingUnitMessage(unit) => {

      log.info(s"Starting to demultiplex: $unit!")

      val DemultiplexingResult(exitStatus, logFile) =
        demultiplexer.demultiplex(unit)

      if (exitStatus == 0)
        sender ! FinishedDemultiplexingProcessingUnitMessage(unit)
      else {
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