package hercules.entities.illumina

import java.io.File
import java.net.URI
import hercules.entities.ProcessingUnit
import hercules.config.processingunit.ProcessingUnitConfig

/**
 * Provides a base for representing a Illumina runfolder.
 */
class IlluminaProcessingUnit(
  processingUnitConfig: ProcessingUnitConfig,
  uri: URI) extends ProcessingUnit {

}