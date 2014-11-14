package hercules.demultiplexing

import java.io.File
import hercules.entities.ProcessingUnit

/**
 * A demultiplexing result.
 *
 * @param unit The unit which was demultiplexed
 * @param success Did we succeed or not?
 * @param info Wrap any information on the process (such as a log text) here.
 */
case class DemultiplexingResult(
    unit: ProcessingUnit,
    success: Boolean,
    info: Option[String]) {

}