package hercules.protocols

import hercules.entities.ProcessingUnit
import hercules.entities.notification.NotificationUnit
import hercules.entities.ProcessingUnit

/**
 * Import this object to gain access to the messaging protocol of
 * Hercules.
 *
 * All messages which are to be sent globally need to be defined in here!
 */
object HerculesMainProtocol {

  //--------------------------------------------------------------
  // GENERAL MESSAGES
  //--------------------------------------------------------------

  /**
   * This trait is the base for all messages in the Hercules application
   * All messages to be parsed need to extend this!
   */
  sealed trait HerculesMessage

  /**
   * Use this to acknowledge e.g. that some work was accepted
   */
  case object Acknowledge extends HerculesMessage

  /**
   * Use this to Reject a something, like a work load. Optionally include a
   * reason.
   * @param reason what something was rejected.
   */
  case class Reject(reason: Option[String] = None) extends HerculesMessage

  /**
   * TODO: This should probably realy be moved out into a MasterProtocol,
   * since it's not a message which all actors are expected to be able to
   * handle. /JD 20141113
   * Request the state from the master, if no unit has been specified
   * the full state should be returned. This can be used for example the details
   * @param unitName the name of the processing unit to look for
   */
  case class RequestMasterState(unitName: Option[String] = None) extends HerculesMessage
  case object PurgeMasterState extends HerculesMessage

  //--------------------------------------------------------------
  // MESSAGES ABOUT FINDING AND FORGETTING PROCESSING UNITS
  //--------------------------------------------------------------

  /**
   * Base trait for processing unit messages. Any message type which relates
   * to a processing unit should extend this.
   */
  sealed trait ProcessingMessage extends HerculesMessage

  /**
   * The base trait for the messages encapsulating the state of the
   * ProcessingUnit, which in turn defines what is to be done with it.
   */
  trait ProcessingUnitMessage extends ProcessingMessage {
    val unit: ProcessingUnit
  }
  /**
   * Used for when the you don't have a ProcessingUnit, but just a name,
   * e.g. when looking up a processing unit to see which state it's in.
   */
  trait ProcessingUnitNameMessage extends ProcessingMessage {
    val unitName: String
  }

  /**
   * Used to request a check if there are any processing units which should
   * be forgotten by the actor. E.g. the ProcessingUnitWatcherActor might
   * want to perform some changes on the file system (or in a database) and
   * then reintroduce it into the general flow.
   */
  case object RequestProcessingUnitMessageToForget extends ProcessingMessage

  /**
   * Used to indicate the a processing unit has been found.
   * @param unit which was found.
   */
  case class FoundProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  /**
   * Used to indicate that a processing unit should be forgotten (the exact
   * meaining of that is determined by the context).
   * @param unitName of processing unit to forget.
   */
  case class ForgetProcessingUnitMessage(unitName: String) extends ProcessingUnitNameMessage

  //--------------------------------------------------------------
  // MESSAGES ABOUT DEMULTIPLEXING
  //--------------------------------------------------------------

  /**
   * Base trait for all demultiplexing messages
   */
  sealed trait DemultiplexingMessage extends ProcessingMessage

  /**
   * Sent to indicate a request for a demultiplexing message, e.g if one wants
   * a new processing unit to demultiplex.
   */
  case object RequestDemultiplexingProcessingUnitMessage extends DemultiplexingMessage

  /**
   * Send to start demultiplexing a processing unit.
   * @param unit processing unit
   */
  case class StartDemultiplexingProcessingUnitMessage(unit: ProcessingUnit) extends DemultiplexingMessage with ProcessingUnitMessage

  /**
   * Send to stop demultiplexing of a processing unit.
   * @param unit processing unit
   */
  case class StopDemultiplexingProcessingUnitMessage(unitName: String) extends DemultiplexingMessage with ProcessingUnitNameMessage

  /**
   * Send to indicate the demultiplexing of a processing unit has finished.
   * @param unit processing unit
   */
  case class FinishedDemultiplexingProcessingUnitMessage(unit: ProcessingUnit) extends DemultiplexingMessage with ProcessingUnitMessage

  /**
   * Send to indicate the demultiplexing of a processing unit has failed.
   * @param unit processing unit
   * @param reason why it failed.
   */
  case class FailedDemultiplexingProcessingUnitMessage(unit: ProcessingUnit, reason: String) extends DemultiplexingMessage with ProcessingUnitMessage

  /**
   * Send to indicate the demultiplexing of a processing unit should be restarted.
   * @param unitName of unit which should be restarted
   */
  case class RestartDemultiplexingProcessingUnitMessage(unitName: String) extends DemultiplexingMessage with ProcessingUnitNameMessage

  /**
   * Send to indicate the demultiplexing of a processing unit should be forgotten,
   * this would typically indicate that demultiplexing should be reinitiated.
   * @param unitName of unit which should be forgotten.
   */
  case class ForgetDemultiplexingProcessingUnitMessage(unitName: String) extends DemultiplexingMessage with ProcessingUnitNameMessage

  //--------------------------------------------------------------
  // MESSAGES ABOUT QUALITY CONTROL
  // TODO None of these messages are used yet. /JD 20141114
  //--------------------------------------------------------------
  /**
   * Start the QC checking process for a demultiplexing unit
   * @param unit processing unit
   */
  case class StartQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  /**
   * Finished the QC checking process for a demultiplexing unit
   * @param unit processing unit
   */
  case class FinishedQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  /**
   * Failed the QC checking process for a demultiplexing unit
   * @param unit processing unit
   */
  case class FailedQCProcessingUnitMessage(unit: ProcessingUnit) extends ProcessingUnitMessage

  //--------------------------------------------------------------
  // MESSAGES NOTIFICATIONS
  //--------------------------------------------------------------

  /**
   * The base trait for the messages encapsulating the notifications to be
   * sent out. Contains the NotificationUnit.
   */
  sealed trait NotificationUnitMessage extends HerculesMessage {
    val unit: NotificationUnit
  }

  /**
   * Send this notification unit
   * @param notificationUnit to send
   */
  case class SendNotificationUnitMessage(unit: NotificationUnit) extends NotificationUnitMessage

  /**
   * This notification unit has been sent
   * @param notificationUnit to send
   */
  case class SentNotificationUnitMessage(unit: NotificationUnit) extends NotificationUnitMessage

  /**
   * Failed in sending this notification unit
   * @param notificationUnit to send
   */
  case class FailedNotificationUnitMessage(unit: NotificationUnit, reason: String) extends NotificationUnitMessage

  //@TODO Extend this with all messages that we should to be able to send!

}