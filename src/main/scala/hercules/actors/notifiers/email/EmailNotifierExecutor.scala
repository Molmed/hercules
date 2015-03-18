package hercules.actors.notifiers.email

import akka.event.LoggingReceive
import hercules.actors.notifiers.NotificationExecutor
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit

/**
 * An executor actor that will send emails using the mixing from the Emailer.
 * Will attempt to pass any EmailNotificationUnit that comes in to the emailer.
 * Will accept messages of type:
 * EmailNotificationUnit
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