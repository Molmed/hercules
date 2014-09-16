package hercules.config.notification

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.util.Hashtable
import java.util.List

object EmailNotificationConfig {

  /** Create and return a EmailNotificationConfig from the supplied 
   *  configuration. If necessary, provide default values for missing options.
   */
  def getEmailNotificationConfig(conf: Config): EmailNotificationConfig = {
    val emailRecipients = conf.getStringList("recipients")  
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
  val emailRecipients: java.util.List[String],
  val emailSender: String,
  val emailSMTPHost: String,
  val emailSMTPPort: Int,
  val emailPrefix: String) extends NotificationConfig {
}
