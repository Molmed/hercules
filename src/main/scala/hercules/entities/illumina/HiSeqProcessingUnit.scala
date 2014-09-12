package hercules.entities.illumina

import java.net.URI

import hercules.config.processingunit.IlluminaProcessingUnitConfig

/**
 * Represent a HiSeq runfolder
 */
case class HiSeqProcessingUnit(
  val processingUnitConfig: IlluminaProcessingUnitConfig,
  val uri: URI)
    extends IlluminaProcessingUnit() {
  
}