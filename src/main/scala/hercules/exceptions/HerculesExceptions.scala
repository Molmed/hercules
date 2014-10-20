package hercules.exceptions

import hercules.entities.ProcessingUnit

object HerculesExceptions {

  sealed trait HerculesException extends Exception
  case class ExternalProgramException(message: String, unit: ProcessingUnit) extends HerculesException

}