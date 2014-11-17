package hercules.actors.masters

import hercules.protocols.HerculesMainProtocol.ProcessingMessage

object MasterStateProtocol {

  // These messages are always to be used when updating the state of the 
  // actor. The reason for this is that these messages need to be persisted
  // to be able to able to replay the actors state.
  sealed trait SetStateMessage

  case class AddToMessageNotYetProcessed(message: Option[ProcessingMessage]) extends SetStateMessage
  case class RemoveFromMessageNotYetProcessed(message: Option[ProcessingMessage]) extends SetStateMessage
  case object PurgeMessagesNotYetProcessed extends SetStateMessage

  case class AddToMessagesInProcessing(message: Option[ProcessingMessage]) extends SetStateMessage
  case class RemoveFromMessagesInProcessing(message: Option[ProcessingMessage]) extends SetStateMessage
  case object PurgeMessagesInProcessing extends SetStateMessage

  case class AddToFailedMessages(message: Option[ProcessingMessage]) extends SetStateMessage
  case class RemoveFromFailedMessages(message: Option[ProcessingMessage]) extends SetStateMessage
  case object PurgeFailedMessages extends SetStateMessage
}
