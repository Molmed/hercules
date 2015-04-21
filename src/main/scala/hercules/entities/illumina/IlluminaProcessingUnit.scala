package hercules.entities.illumina

import java.io.{ PrintWriter, File }
import java.net.URI

import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.entities.ProcessingUnit
import hercules.utils.VersionUtils
import spray.json.JsValue

object IlluminaProcessingUnit {
  val nameOfIndicatorFile = "found"
}

/**
 * Provides a base for representing a Illumina runfolder.
 */
trait IlluminaProcessingUnit extends ProcessingUnit {

  val processingUnitConfig: IlluminaProcessingUnitConfig
  val uri: URI

  /**
   * Values to be mapped to json representation.
   * Override in implementing class to add more information.
   * @return the key value pairs representing the object in json
   */
  override protected def mappedValues: Map[String, JsValue] = {
    super.mappedValues.updated("config", processingUnitConfig.toJson)
  }

  /**
   * Get the name from the runfolder directory name
   */
  def name: String = new File(uri.getPath).getName

  /**
   * The indicator file dropped when the processing unit is marked as found.
   */
  private def indicatorFile: File =
    new File(uri.getPath + File.separator + IlluminaProcessingUnit.nameOfIndicatorFile)

  /**
   * Is found is true if the indicator file exists.
   */
  def isFound: Boolean = indicatorFile.exists()

  /**
   * Marking as found means creating the indicator file.
   * Will propagate any exceptions thrown while trying to create
   * the found file.
   */
  def markAsFound: Boolean = {
    try {
      val printWriter = new PrintWriter(indicatorFile)
      printWriter.println("The version of hercules was: " + VersionUtils.herculesVersion)
      printWriter.close()
      true
    } catch {
      case e: Exception => throw e
    }
  }

  /**
   * Marking as not found means removing the indicator file.
   */
  def markNotFound: Boolean = indicatorFile.delete

}
