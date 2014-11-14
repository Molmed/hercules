package hercules.exceptions

import hercules.entities.ProcessingUnit

/**
 * Hercules exceptions
 */
object HerculesExceptions {

  /**
   * Base trait for a HerculesExceptions
   */
  sealed trait HerculesException extends Exception

  /**
   * Exception thrown when an external program fails when processing a unit
   *
   * @param message The exception message
   * @param unit which was being processed by the external program.
   */
  case class ExternalProgramException(message: String, unit: ProcessingUnit) extends HerculesException

}