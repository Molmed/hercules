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

  /**
   *  Get/Set the discovered state of this ProcessingUnit
   *  @param state If true, set the unit as found by creating an indicator file.
   *  If false, set the unit as not found by removing the indicator file.
   *  If None, return the presence of the indicator file.
   *  @return The (possibly updated) state of the ProcessingUnit.
   *  Should be checked to verify that the expected state change took place.
   */
  def discovered(state: Option[Boolean] = None): Boolean = {
    val indicatorFile = new File(uri.getPath + File.separator + "found")
    if (state.isEmpty) indicatorFile.exists()
    else {
      if (state.get) indicatorFile.createNewFile
      else indicatorFile.delete
    }
    discovered()
  }
}