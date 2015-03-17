package hercules.actors.notifiers.email

import akka.actor.{ ActorLogging, Actor }
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit
import hercules.protocols.HerculesMainProtocol.{ FailedNotificationUnitMessage, SentNotificationUnitMessage }
import scala.util.{ Failure, Success }

/**
 * Add email capabilities to an actor.
 * Created by johda411 on 2015-03-17.
 */
trait Emailer {

  this: Actor with ActorLogging =>

  /**
   * Send an email, and get back to the parent actor with the results.
   * The success vs. failure are communicated via messages so to catch them
   * make sure that the implementing actor can handle them.
   * @param email to send
   * @param emailConfig to use
   */
  def sendEmail(email: EmailNotificationUnit, emailConfig: EmailNotificationConfig): Unit = {

    // Keep the reference to sender available for the future
    val parentActor = sender

    import context.dispatcher

    val emailDelivery = email.sendNotification(
      emailConfig.recipients,
      emailConfig.sender,
      emailConfig.prefix,
      emailConfig.smtpHost,
      emailConfig.smtpPort)

    emailDelivery onComplete {
      case Success(_) => {
        log.debug(email + " sent successfully")
        parentActor ! SentNotificationUnitMessage(email)
      }
      case Failure(t) => {
        log.warning("Sending " + email + " failed for the " + (email.attempts + 1) + " time with message: " + t.getMessage)
        parentActor ! FailedNotificationUnitMessage(email.copy(attempts = email.attempts + 1), t.getMessage)
      }
    }
  }

}
