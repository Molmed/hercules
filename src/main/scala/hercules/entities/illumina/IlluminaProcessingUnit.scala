package hercules.entities.illumina

import java.io.File
import java.net.URI

import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.entities.ProcessingUnit

/**
 * Provides a base for representing a Illumina runfolder.
 */
trait IlluminaProcessingUnit extends ProcessingUnit {

  val processingUnitConfig: IlluminaProcessingUnitConfig
  val uri: URI
  def name: String = new File(uri.getPath).getName
  private def indicatorFile: File = new File(uri.getPath + File.separator + "found")
  def isFound: Boolean = indicatorFile.exists()
  def markAsFound: Boolean = indicatorFile.createNewFile
  def markNotFound: Boolean = indicatorFile.delete

}
