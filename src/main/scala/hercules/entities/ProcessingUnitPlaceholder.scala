package hercules.entities

import java.io.File

case class ProcessingUnitPlaceholder(val name: String) extends ProcessingUnit {
  val uri = new File(name).toURI
  val isFound: Boolean = false
}
