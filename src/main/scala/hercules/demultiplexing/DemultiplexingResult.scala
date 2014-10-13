package hercules.demultiplexing

import java.io.File
import hercules.entities.ProcessingUnit

case class DemultiplexingResult(unit: ProcessingUnit, success: Boolean, logText: Option[String]) {

}