package hercules.demultiplexing

import hercules.entities.ProcessingUnit

trait Demultiplexer {
  def demultiplex(unit: ProcessingUnit): DemultiplexingResult
  def cleanup(unit: ProcessingUnit): Unit
}