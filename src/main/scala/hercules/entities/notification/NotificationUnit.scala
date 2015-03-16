package hercules.entities.notification

import hercules.protocols.NotificationChannelProtocol._

/**
 * Provides a base for representing a notification. Subclasses extends this and
 * implements the means of delivery
 * @param message to send
 * @param channel to send it on.
 * @param attempts this many attempts have been made at sending this notification
 */
class NotificationUnit(
  val message: String,
  val channel: NotificationChannel,
  val attempts: Int = 0) {}
