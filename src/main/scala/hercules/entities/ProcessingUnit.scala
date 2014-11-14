package hercules.entities

import java.net.URI

/**
 * A atomic unit to be processed by Hercules. E.g. a Illumina runfolder.
 */
trait ProcessingUnit {

  /**
   * The URI identifying the processing unit
   */
  val uri: URI

  /**
   * The name of the processing unit
   */
  def name: String

  /**
   * Specify if this processing unit has already been found.
   */
  def isFound: Boolean

}