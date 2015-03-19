package hercules.config.notification

import com.typesafe.config.{ ConfigFactory, Config }
import scala.collection.JavaConversions._
import hercules.protocols.NotificationChannelProtocol._

object EmailNotificationConfig {

  def apply(): EmailNotificationConfig = {
    val baseConfig = ConfigFactory.load()
    getEmailNotificationConfig(baseConfig.getConfig("notifications.email"))
  }

  /**
   * Create and return a EmailNotificationConfig from the supplied
   *  configuration.
   * @param conf A Typesafe Config
   * @return Returns a EmailNotificationConfig
   */
  def getEmailNotificationConfig(conf: Config): EmailNotificationConfig = {
    val emailRecipients = asScalaBuffer(conf.getStringList("recipients")).toSeq
    val emailSender = conf.getString("sender")
    val emailSMTPHost = conf.getString("smtp_host")
    val emailSMTPPort = conf.getInt("smtp_port")
    val emailPrefix = conf.getString("prefix")
    val emailChannels = asScalaBuffer(
      conf.getStringList("channels")).toSeq.map(
        stringToChannel)
    val emailNumRetries = conf.getInt("num_retries")
    val emailRetryInterval = conf.getInt("retry_interval")
    new EmailNotificationConfig(
      emailRecipients,
      emailSender,
      emailSMTPHost,
      emailSMTPPort,
      emailPrefix,
      emailNumRetries,
      emailRetryInterval,
      emailChannels)
  }
}

/**
 * Base class for configuring an email notification
 *
 * @param recipients
 * @param sender
 * @param smtpHost
 * @param smtpPort
 * @param prefix
 * @param numRetries
 * @param retryInterval
 * @param channels
 */
class EmailNotificationConfig(
    val recipients: Seq[String],
    val sender: String,
    val smtpHost: String,
    val smtpPort: Int,
    val prefix: String,
    val numRetries: Int,
    val retryInterval: Int,
    val channels: Seq[NotificationChannel]) extends NotificationConfig {
}
