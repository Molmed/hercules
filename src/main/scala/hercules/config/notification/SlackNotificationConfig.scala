package hercules.config.notification

import com.typesafe.config.{ ConfigFactory, Config }
import hercules.protocols.NotificationChannelProtocol._

import scala.collection.JavaConversions._

/**
 * Utility functions associated with creating a SlackNotificationConfig.
 * Created by johda411 on 2015-03-18.
 */
object SlackNotificationConfig {
  /**
   * Get a default config as specified in the application.conf file.
   * @return a slack notifications config
   */
  def apply(): SlackNotificationConfig = {
    getSlackNotificationConfig(ConfigFactory.load().getConfig("notifications.slack"))
  }

  /**
   * Load a SlackNotificationConfig from the specified config instance
   * @param conf the Config instance to load from
   * @return A SlackNotificationConfig
   */
  def getSlackNotificationConfig(conf: Config): SlackNotificationConfig = {
    new SlackNotificationConfig(
      channels = asScalaBuffer(conf.getStringList("channels")).toSeq.map(stringToChannel),
      retryInterval = conf.getInt("num_retries"),
      numRetries = conf.getInt("retry_interval"),
      slackEndPoint = conf.getString("slack_endpoint"),
      slackChannel = conf.getString("slack_channel"),
      slackUserName = conf.getString("slack_user"),
      iconEmoji = conf.getString("icon_emoji")
    )
  }
}

/**
 * Configure which (Hercules) channels should be passed on to the Slack notifications
 * and how to deal with retries.
 * @param channels the Notifications channels to use.
 * @param retryInterval
 * @param numRetries
 */
class SlackNotificationConfig(
  override val channels: Seq[NotificationChannel],
  override val retryInterval: Int,
  override val numRetries: Int,
  val slackEndPoint: String,
  val slackChannel: String,
  val slackUserName: String,
  val iconEmoji: String) extends NotificationConfig {}
