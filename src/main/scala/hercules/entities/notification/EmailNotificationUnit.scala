package hercules.entities.notification

import courier._
import Defaults._
import scala.concurrent.Future
import hercules.protocols.NotificationChannelProtocol._

object EmailNotificationUnit {
  def wrapNotificationUnit(unit: NotificationUnit): EmailNotificationUnit = EmailNotificationUnit(unit.message, unit.channel)
}

/**
 * Provides a base for representing an email notification unit
 */
case class EmailNotificationUnit(
    override val message: String,
    override val channel: NotificationChannel,
    val attempts: Int = 0) extends NotificationUnit(message, channel) {

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
