package hercules.test.utils

import java.io.File

import hercules.entities.ProcessingUnit

/**
 * Used as an easy mock for processing units.
 * Created by johda411 on 2015-04-24.
 */
case class ProcessingUnitPlaceholder(val name: String) extends ProcessingUnit {
  val uri = new File(name).toURI
  val isFound: Boolean = true
}
