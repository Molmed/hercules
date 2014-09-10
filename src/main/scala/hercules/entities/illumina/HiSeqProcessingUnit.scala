package hercules.entities.illumina

import java.net.URI

import hercules.config.processingunit.IlluminaProcessingUnitConfig

/**
 * Represent a HiSeq runfolder
 */
case class HiSeqProcessingUnit(
  override val processingUnitConfig: IlluminaProcessingUnitConfig,
  override val uri: URI)
    extends IlluminaProcessingUnit(processingUnitConfig, uri) {

}