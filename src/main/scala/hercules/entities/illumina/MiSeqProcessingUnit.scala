package hercules.entities.illumina

import java.io.File
import java.net.URI
import hercules.config.processingunit.ProcessingUnitConfig

/**
 * Represent a MiSeq runfolder
 */
case class MiSeqProcessingUnit(
  processingUnitConfig: ProcessingUnitConfig,
  uri: URI)
    extends IlluminaProcessingUnit(processingUnitConfig, uri) {}