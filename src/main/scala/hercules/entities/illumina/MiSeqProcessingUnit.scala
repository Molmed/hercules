package hercules.entities.illumina

import java.net.URI

import hercules.config.processingunit.IlluminaProcessingUnitConfig

/**
 * Represent a MiSeq runfolder
 */
case class MiSeqProcessingUnit(
  val processingUnitConfig: IlluminaProcessingUnitConfig,
  val uri: URI)
    extends IlluminaProcessingUnit() {}