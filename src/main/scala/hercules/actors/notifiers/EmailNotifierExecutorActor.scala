package hercules.actors.notifiers

import akka.actor.Props
import akka.event.LoggingReceive
import hercules.actors.HerculesActor
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit
import hercules.protocols.HerculesMainProtocol
import scala.util.{ Success, Failure }

/**
 * Provided factory methods for creating EmailNotifierExecutorActors
 */
object EmailNotifierExecutorActor {

  /**
   * Get a props for creating a EmailNotifierExecutorActor.
   * @param emailConfig
   * @return Props for creating a EmailNotifierExecutorActor
   */
  def props(emailConfig: EmailNotificationConfig): Props = {
    Props(new EmailNotifierExecutorActor(emailConfig))
  }

}

/**
 * A actor which can send emails based on a SendNotificationUnitMessage.
 *
 * @param emailConfig
 */
class EmailNotifierExecutorActor(
    emailConfig: EmailNotificationConfig) extends NotifierActor {

  import HerculesMainProtocol._
  import context.dispatcher

  def receive = LoggingReceive {
    case message: SendNotificationUnitMessage => {
      message.unit match {
        case unit: EmailNotificationUnit => {
          // Keep the reference to sender available for the future
          val parentActor = sender
          // If we manage to send the message, send a confirmation
          val emailDelivery = unit.sendNotification(
            emailConfig.recipients,
            emailConfig.sender,
            emailConfig.prefix,
            emailConfig.smtpHost,
            emailConfig.smtpPort)
          emailDelivery onComplete {
            case Success(_) => {
              log.debug(unit + " sent successfully")
              parentActor ! SentNotificationUnitMessage(unit)
            }
            case Failure(t) => {
              log.warning("Sending " + unit + " failed for the " + (unit.attempts + 1) + " time")
              parentActor ! FailedNotificationUnitMessage(unit.copy(attempts = unit.attempts + 1), t.getMessage)
            }
          }
        }
        case _ =>
          // We don't know how to handle non-EmailNotificationUnits
          log.warning("An " + this.getClass.getSimpleName + " does not know how to send a " + message.unit.getClass.getSimpleName)
          sender ! FailedNotificationUnitMessage(message.unit, "Unhandled unit type: " + message.unit.getClass.getSimpleName)
      }
    }
  }
}
