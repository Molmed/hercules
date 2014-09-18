package hercules.config.notification

import hercules.protocols.NotificationChannelProtocol._

/**
 * Base class for configuring a notification
 */
abstract class NotificationConfig {
  val channels: Seq[NotificationChannel]
}