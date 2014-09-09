package hercules.entities.illumina

import java.io.File
import java.net.URI
import hercules.config.processingunit.ProcessingUnitConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig

/**
 * Represent a MiSeq runfolder
 */
case class MiSeqProcessingUnit(
  override val processingUnitConfig: IlluminaProcessingUnitConfig,
  override val uri: URI)
    extends IlluminaProcessingUnit(processingUnitConfig, uri) {}