package hercules.config.notification

import hercules.protocols.NotificationChannelProtocol._

/**
 * Base class for configuring a notification
 * @param channels the Notifications channels to use.
 * @param numRetries to do before giving up on sending the notification
 * @param retryInterval seconds to wait between retries
 */
abstract class NotificationConfig {
  val channels: Seq[NotificationChannel]
  val numRetries: Int
  val retryInterval: Int
}