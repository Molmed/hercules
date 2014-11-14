package hercules.entities

import hercules.config.processingunit.ProcessingUnitConfig
import hercules.config.processingunit.ProcessingUnitFetcherConfig

/**
 * A base trait for the ProcessingUnitFetcher
 *
 * Setup it's exact return type by setting the types in the implementing class
 *
 */
trait ProcessingUnitFetcher {

  type FetherConfigType <: ProcessingUnitFetcherConfig
  type ProcessingUnitType <: ProcessingUnit

  /**
   * Return if the unit is ready to be processed yet.
   *
   * @param unit you're checking.
   * @return If the processing unit is ready for continued processing
   */
  def isReadyForProcessing(unit: ProcessingUnitType): Boolean

  /**
   * Check for new ready processing units based on your config.
   *
   * @param config
   * @return the processing units found.
   */
  def checkForReadyProcessingUnits(
    config: FetherConfigType): Seq[ProcessingUnitType]
}