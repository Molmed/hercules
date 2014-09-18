package hercules.test.utils

import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorRef

/**
 * Use this class to create a fake parent class to pass messages through
 * which should have been sent to the master.
 * It will simply take any messages from the child and forward them.
 */
class StepParent(childToCreate: Props, probe: ActorRef, role: String = "child") extends Actor {
  val child = context.actorOf(childToCreate, name = role)
  def receive = {
    case msg => probe.tell(msg, sender)
  }
}