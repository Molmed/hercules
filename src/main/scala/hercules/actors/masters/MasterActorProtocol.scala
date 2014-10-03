package hercules.actors.masters

import hercules.protocols.HerculesMainProtocol.ProcessingUnitMessage

object MasterStateProtocol {

  // These messages are always to be used when updating the state of the 
  // actor. The reason for this is that these messages need to be persisted
  // to be able to able to replay the actors state.
  sealed trait SetStateMessage

  case class AddToMessageNotYetProcessed(message: ProcessingUnitMessage) extends SetStateMessage
  case class RemoveFromMessageNotYetProcessed(message: ProcessingUnitMessage) extends SetStateMessage

  case class AddToFailedMessages(message: ProcessingUnitMessage) extends SetStateMessage
  case class RemoveFromFailedMessages(message: ProcessingUnitMessage) extends SetStateMessage
}
