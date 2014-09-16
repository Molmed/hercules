package hercules.config.notification

import com.typesafe.config.Config
import java.util.List
import scala.collection.JavaConversions._

object EmailNotificationConfig {

  /** Create and return a EmailNotificationConfig from the supplied 
   *  configuration. If necessary, provide default values for missing options.
   */
  def getEmailNotificationConfig(conf: Config): EmailNotificationConfig = {
    val emailRecipients = asScalaBuffer(conf.getStringList("recipients")).toSeq
    val emailSender = conf.getString("sender")
    val emailSMTPHost = conf.getString("smtp_host")
    val emailSMTPPort = conf.getInt("smtp_port")
    val emailPrefix = conf.getString("prefix")
    new EmailNotificationConfig(
      emailRecipients,
      emailSender,
      emailSMTPHost,
      emailSMTPPort,
      emailPrefix
    )
  }
  
}

/**
 * Base class for configuring an email notification
 */
case class EmailNotificationConfig(
  val emailRecipients: Seq[String],
  val emailSender: String,
  val emailSMTPHost: String,
  val emailSMTPPort: Int,
  val emailPrefix: String) extends NotificationConfig {
}
