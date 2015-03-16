package hercules.actors.notifiers.email

import akka.event.LoggingReceive
import hercules.actors.notifiers.NotificationExecutor
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit

/**
 * TODO Write docs!
 */
trait EmailNotifierExecutorActor {
  this: NotificationExecutor with Emailer =>

  def emailConfig: EmailNotificationConfig

  import hercules.protocols.HerculesMainProtocol._

  def receive = LoggingReceive {
    case message: SendNotificationUnitMessage => {
      val emailNotificationUnit = EmailNotificationUnit.wrapNotificationUnit(message.unit)
      sendEmail(emailNotificationUnit, emailConfig)
    }
  }
}