package hercules.protocols

import hercules.entities.ProcessingUnit

/**
 * Import this object to gain access to the messaging protocol of
 * Hercules.
 * All messages which are to be sent globally need to be defined in here!
 */
object HerculesMainProtocol {

  /**
   * This trait is the base for all messages in the Hercules application
   * All messages to be parsed need to extend this!
   */
  sealed trait HerculesMessage

  case object Start extends HerculesMessage
  case object Stop extends HerculesMessage
  case object Restart extends HerculesMessage

  case class StringMessage(s: String) extends HerculesMessage
  
  /**
   * The base trait for the messages encapsulating the state of the
   * ProcessingUnit, which in turn defines what is to be done with it.
   */
  trait ProcessingUnitMessage extends HerculesMessage {
    val unit: ProcessingUnit
  }

  case class FoundProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  case class StartDemultiplexingProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage
  case class FinishedDemultiplexingProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage
  case class FailedDemultiplexingProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  case class StartQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage
  case class FinishedQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage
  case class FailedQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  //@TODO Extend this with all messages that we should to be able to send!

}