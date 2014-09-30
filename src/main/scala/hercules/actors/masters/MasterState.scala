package hercules.actors.masters

import hercules.protocols.HerculesMainProtocol.ProcessingUnitMessage
import hercules.entities.ProcessingUnit
import hercules.actors.masters.MasterStateProtocol._

case class MasterState(
    val messagesNotYetProcessed: Set[ProcessingUnitMessage] = Set(),
    val messagesInProcessing: Set[ProcessingUnitMessage] = Set(),
    val failedMessages: Set[ProcessingUnitMessage] = Set()) {

  /**
   * Manipulate the state of the message sets depending on what message has been
   * sent
   * @param x the SetStateMessage describing the message to add or remove
   * @return the MasterState after the manipulation
   */
  def manipulateState(x: SetStateMessage): MasterState = {
    x match {
      case AddToMessageNotYetProcessed(message) =>
        this.copy(messagesNotYetProcessed = messagesNotYetProcessed + message)

      case RemoveFromMessageNotYetProcessed(message) =>
        this.copy(messagesNotYetProcessed = messagesNotYetProcessed - message)

      case AddToFailedMessages(message) =>
        this.copy(failedMessages = failedMessages + message)

      case RemoveFromFailedMessages(message) =>
        this.copy(failedMessages = failedMessages - message)
    }
  }

  /**
   * Gets the master state as is, or filtered for the unitName option.
   * @param unitName set to None to get state for all units
   * @return a MasteState filtered for unitName (if present)
   */
  def findStateOfUnit(unitName: Option[String]): MasterState = {
    if (unitName.isDefined)
      MasterState(
        messagesNotYetProcessed =
          messagesNotYetProcessed.filter(p => p.unit.name == unitName),
        messagesInProcessing =
          messagesInProcessing.filter(p => p.unit.name == unitName),
        failedMessages =
          failedMessages.filter(p => p.unit.name == unitName))
    else
      this
  }

  //@TODO It whould be awesome to attach a to Json method here to make it
  // easy to drop this to json from the REST API later. /JD 20140929
  def toJson = ???

}