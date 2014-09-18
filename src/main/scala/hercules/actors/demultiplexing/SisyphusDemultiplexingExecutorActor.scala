package hercules.actors.demultiplexing

import akka.actor.Props
import hercules.actors.HerculesActor
import hercules.protocols.HerculesMainProtocol._
import hercules.external.program.Sisyphus
import scala.io.Source

object SisyphusDemultiplexingExecutorActor {
  def props(): Props = Props(new SisyphusDemultiplexingExecutorActor())
}

/**
 * Concrete executor implementation for demultiplexing using Sisyphus
 * This one can lock while doing it work.
 */
class SisyphusDemultiplexingExecutorActor extends HerculesActor {

  def receive = {
    case StartDemultiplexingProcessingUnitMessage(unit) => {

      log.info("Starting a sisyphus instance!")
      
      val sisyphusInstance = new Sisyphus()
      val (exitStatus, logFile) = sisyphusInstance.run(unit)
      if (exitStatus == 0)
        sender ! FinishedDemultiplexingProcessingUnitMessage(unit)
      else {
        sisyphusInstance.cleanup(unit)
        val logText = Source.fromFile(logFile).getLines.mkString
        sender ! FailedDemultiplexingProcessingUnitMessage(unit, logText)
      }

    }

  }

}