package hercules.config.notification

import com.typesafe.config.{ ConfigFactory, Config }
import hercules.protocols.NotificationChannelProtocol._

import scala.collection.JavaConversions._

/**
 * Factory functions to create default and custom NGIPipelineNotificationConfigs,
 * where the default configurations are picked up from the application.conf
 * Created by johda411 on 2015-03-20.
 */
object NGIPipelineNotificationConfig {

  def apply(): NGIPipelineNotificationConfig = {
    getNGIPipelineNotificationConfig(ConfigFactory.load().getConfig("notifications.ngipipeline"))
  }

  def getNGIPipelineNotificationConfig(conf: Config): NGIPipelineNotificationConfig = {
    new NGIPipelineNotificationConfig(
      channels = asScalaBuffer(conf.getStringList("channels")).toSeq.map(stringToChannel),
      retryInterval = conf.getInt("num_retries"),
      numRetries = conf.getInt("retry_interval"),
      host = conf.getString("host"),
      port = conf.getInt("port"))
  }

}

/**
 * A configuration for communicating with the NGI pipline
 * @param channels the Notifications channels to use.
 * @param retryInterval seconds to wait between retries
 * @param numRetries to do before giving up on sending the notification
 */
class NGIPipelineNotificationConfig(override val channels: Seq[NotificationChannel],
                                    override val retryInterval: Int,
                                    override val numRetries: Int,
                                    val host: String,
                                    val port: Int) extends NotificationConfig() {}
