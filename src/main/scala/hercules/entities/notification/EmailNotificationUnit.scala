package hercules.entities.notification

import courier._
import Defaults._
import scala.concurrent.Future
import hercules.protocols.NotificationChannelProtocol._

/**
 * Provides utility functions for the EmailNotificationUnit
 */
object EmailNotificationUnit {

  /**
   * Wrapp a notification using as a EmailNotificationUnit
   * @param unit
   * @return A EmailNotificationUnit
   */
  def wrapNotificationUnit(unit: NotificationUnit): EmailNotificationUnit =
    EmailNotificationUnit(unit.message, unit.channel)
}

/**
 * Container class for NotificationUnits to be sent by email.
 *
 * @param message
 * @param channel
 * @param attempts
 */
case class EmailNotificationUnit(
    override val message: String,
    override val channel: NotificationChannel,
    val attempts: Int = 0) extends NotificationUnit(message, channel) {

  /**
   * Send this notification by email.
   * @param recipients
   * @param sender
   * @param prefix To be used in the subject header, e.g. [HERCULES]
   * @param smtpHost
   * @param smtpPort
   * @return a future wrapped unit (which can be used to wait for the mail to
   * be sent.
   */
  def sendNotification(
    recipients: Seq[String],
    sender: String,
    prefix: String,
    smtpHost: String,
    smtpPort: Int): Future[Unit] = {
    // Create an envelope without specifying the recipients
    val blankEnvelope = Envelope.from(addr(sender).addr)
      .subject(prefix + " " + channel + " " + this.getClass.getName)
      .content(Text(message))
    // Add recipients on by one 
    val envelope = recipients.foldLeft(blankEnvelope)((tEnv, recipient) => tEnv.to(addr(recipient).addr))
    // Create the Mailer and send the envelope, returning a Future
    val mailer = Mailer(smtpHost, smtpPort)()
    mailer(envelope)
  }

}
