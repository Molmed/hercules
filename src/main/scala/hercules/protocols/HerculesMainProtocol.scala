package hercules.protocols

import hercules.entities.ProcessingUnit
import hercules.entities.notification.NotificationUnit
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

  case object Acknowledge
  case class Reject(reason: Option[String] = None)

  case class StringMessage(s: String) extends HerculesMessage

  /**
   * Request the state from the master, if no unit has been specified
   * the full state should be returned. This can be used for example the details
   * @param unitName the name of the processing unit to look for
   */
  case class RequestMasterState(unitName: Option[String] = None) extends HerculesMessage

  /**
   * The base trait for the messages encapsulating the state of the
   * ProcessingUnit, which in turn defines what is to be done with it.
   */
  trait ProcessingUnitMessage extends HerculesMessage {
    val unit: ProcessingUnit
  }

  case class FoundProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  sealed trait DemultiplexingMessage extends HerculesMessage
  case object RequestDemultiplexingProcessingUnitMessage extends DemultiplexingMessage
  case class StartDemultiplexingProcessingUnitMessage(unit: ProcessingUnit) extends DemultiplexingMessage with ProcessingUnitMessage
  case class StopDemultiplexingProcessingUnitMessage(unitName: String) extends DemultiplexingMessage
  case class FinishedDemultiplexingProcessingUnitMessage(unit: ProcessingUnit) extends DemultiplexingMessage with ProcessingUnitMessage
  case class FailedDemultiplexingProcessingUnitMessage(unit: ProcessingUnit, reason: String) extends DemultiplexingMessage with ProcessingUnitMessage
  case class RestartDemultiplexingProcessingUnitMessage(unitName: String) extends DemultiplexingMessage
  case class ForgetDemultiplexingProcessingUnitMessage(unitName: String) extends DemultiplexingMessage

  case class StartQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage
  case class FinishedQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage
  case class FailedQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  /**
   * The base trait for the messages encapsulating the notifications to be
   * sent out. Contains the NotificationUnit.
   */
  sealed trait NotificationUnitMessage extends HerculesMessage {
    val unit: NotificationUnit
  }

  case class SendNotificationUnitMessage(unit: NotificationUnit) extends NotificationUnitMessage
  case class SentNotificationUnitMessage(unit: NotificationUnit) extends NotificationUnitMessage
  case class FailedNotificationUnitMessage(unit: NotificationUnit, reason: String) extends NotificationUnitMessage

  //@TODO Extend this with all messages that we should to be able to send!

}