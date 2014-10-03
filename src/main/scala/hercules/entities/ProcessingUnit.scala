package hercules.entities

import java.net.URI

/**
 * A atomic unit to be processed by Hercules. E.g. a Illumina runfolder.
 */
trait ProcessingUnit {

  val uri: URI
  def name: String

}