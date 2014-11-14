package hercules.entities.illumina

import java.net.URI

import hercules.config.processingunit.IlluminaProcessingUnitConfig

/**
 * Represent a HiSeq runfolder
 * @param processingUnitConfig
 * @param uri pointing to the runfolder
 */
case class HiSeqProcessingUnit(
  val processingUnitConfig: IlluminaProcessingUnitConfig,
  val uri: URI)
    extends IlluminaProcessingUnit {

}