package hercules.demultiplexing

import scala.concurrent.{ Future, ExecutionContext }
import hercules.entities.ProcessingUnit

trait Demultiplexer {
  def demultiplex(unit: ProcessingUnit)(implicit executor: ExecutionContext): Future[DemultiplexingResult]
  def cleanup(unit: ProcessingUnit): Unit
}