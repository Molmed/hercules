package hercules.actors.notifiers

import akka.actor.Actor
import akka.actor.ActorLogging
import hercules.protocols.HerculesMainProtocol.HerculesMessage
import hercules.entities.notification.NotificationUnit

object NotifierActor {

  object NotifierStateProtocol {

    // These messages are always to be used when updating the state of the 
    // actor. 
    sealed trait NotificationStateMessage

    case class AddToFailedNotifications(unit: NotificationUnit) extends NotificationStateMessage
    case class RemoveFromFailedNotifications(unit: NotificationUnit) extends NotificationStateMessage
  }
}

/**
 * Send notifications of events (e.g. email them or push cards around on a
 * trello board). All specific implementations of notifier Actors should extend
 * the NotifierActor trait. Note that it does not extend the HerculesActor since
 * that would cause circular dependencies (?)
 */
trait NotifierActor extends Actor with ActorLogging
