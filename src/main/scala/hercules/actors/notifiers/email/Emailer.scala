package hercules.actors.notifiers.email

import scala.concurrent._
import akka.actor.{ ActorLogging, Actor }
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit
import hercules.protocols.HerculesMainProtocol.{ FailedNotificationUnitMessage, SentNotificationUnitMessage }
import scala.util.{ Failure, Success }

/**
 * TODO Write docs!
 * Created by johda411 on 2015-03-17.
 */
trait Emailer {

  this: Actor with ActorLogging =>

  /**
   * TODO Write docs!
   * @param email
   * @param emailConfig
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
