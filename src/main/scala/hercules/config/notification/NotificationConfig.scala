package hercules.config.notification

import hercules.protocols.NotificationChannelProtocol._

/**
 * Base class for configuring a notification
 * @param channels the Notifications channels to use.
 */
abstract class NotificationConfig {
  val channels: Seq[NotificationChannel]
  val numRetries: Int
  val retryInterval: Int
}