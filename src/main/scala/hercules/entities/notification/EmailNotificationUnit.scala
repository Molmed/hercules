package hercules.entities.notification

import java.util.List
import courier._, Defaults._
import scala.concurrent.Future
import scala.collection.JavaConversions._

object EmailNotificationUnit {
  
  def sendNotification(
    unit: EmailNotificationUnit,
    recipients: java.util.List[String],
    sender: String,
    prefix: String,
    smtpHost: String,
    smtpPort: Int): Future[Unit] = {
    	val mailer = Mailer(smtpHost,smtpPort)()
    	mailer(
    		Envelope.from(addr(sender).addr)
//        	.to(scala.collection.JavaConversions.asScalaBuffer(recipients).toSeq.map(to => addr(to).addr))
        	.to(addr(recipients.get(0)).addr)
        	.subject(prefix + " " + unit.getClass.getName)
        	.content(Text(unit.message))
        )
  	}
  
}
/**
 * Provides a base for representing an email notification unit
 */
class EmailNotificationUnit(
  val message: String,
  var attempts: Int = 0) extends NotificationUnit {}
