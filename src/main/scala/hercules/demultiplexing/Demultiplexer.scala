package hercules.demultiplexing

import scala.concurrent.{ Future, ExecutionContext }
import hercules.entities.ProcessingUnit

/**
 * Base trait for Demultiplexers - all demultiplexer should implement this trait
 */
trait Demultiplexer {
  def demultiplex(unit: ProcessingUnit)(implicit executor: ExecutionContext): Future[DemultiplexingResult]
  def cleanup(unit: ProcessingUnit): Unit
}