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

  /**
   * Get the name from the runfolder directory name
   */
  def name: String = new File(uri.getPath).getName

  /**
   * The indicator file dropped when the processing unit is marked as found.
   */
  private def indicatorFile: File =
    new File(uri.getPath + File.separator + "found")

  /**
   * Is found is true if the indicator file exists.
   */
  def isFound: Boolean = indicatorFile.exists()

  /**
   * Marking as found means creating the indicator file.
   */
  def markAsFound: Boolean = indicatorFile.createNewFile

  /**
   * Marking as not found means removing the indicator file.
   */
  def markNotFound: Boolean = indicatorFile.delete

}
