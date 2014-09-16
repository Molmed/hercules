package hercules.entities.notification

import courier._
import Defaults._
import scala.concurrent.Future

object EmailNotificationUnit {
  
  def sendNotification(
    unit: EmailNotificationUnit,
    recipients: Seq[String],
    sender: String,
    prefix: String,
    smtpHost: String,
    smtpPort: Int): Future[Unit] = {
      // Create an envelope without specifying the recipients
      val blankEnvelope = Envelope.from(addr(sender).addr)
        	.subject(prefix + " " + unit.getClass.getName)
        	.content(Text(unit.message))
      // Add recipients on by one 
      val envelope = recipients.foldLeft(blankEnvelope)((tEnv,recipient) => tEnv.to(addr(recipient).addr))
      // Create the Mailer and send the envelope, returning a Future
    	val mailer = Mailer(smtpHost,smtpPort)()
    	mailer(envelope)
  	}  
}

/**
 * Provides a base for representing an email notification unit
 */
class EmailNotificationUnit(
  val message: String,
  var attempts: Int = 0) extends NotificationUnit {}
 