package hercules.entities

import hercules.config.processingunit.ProcessingUnitConfig
import hercules.config.processingunit.ProcessingUnitFetcherConfig

trait ProcessingUnitFetcher {
  type FetherConfigType <: ProcessingUnitFetcherConfig
  type ProcessingUnitType <: ProcessingUnit

  def isReadyForProcessing(unit: ProcessingUnitType): Boolean
  def checkForReadyProcessingUnits(
    config: FetherConfigType): Seq[ProcessingUnitType]
}