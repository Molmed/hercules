package hercules.actors.notifiers;

import hercules.entities.notification.NotificationUnit
import NotifierActor.NotifierStateProtocol._

case class NotifierState(
    val failedNotifications: Set[NotificationUnit] = Set()) {

  def manipulateState(op: NotificationStateMessage): NotifierState =
    op match {
      case message: AddToFailedNotifications =>
        this.copy(failedNotifications + message.unit)
      case message: RemoveFromFailedNotifications =>
        this.copy(failedNotifications - message.unit)
    }
}
