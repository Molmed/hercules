package hercules.entities

import hercules.config.processingunit.ProcessingUnitConfig
import hercules.config.processingunit.ProcessingUnitFetcherConfig

trait ProcessingUnitFetcher[A <: ProcessingUnitFetcherConfig, B <: ProcessingUnit] {
  def isReadyForProcessing(unit: B): Boolean
  def checkForReadyProcessingUnits(
    config: A): Seq[B]
}