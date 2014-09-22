package hercules.entities.notification

import hercules.protocols.NotificationChannelProtocol._

/**
 * Provides a base for representing a notification. Subclasses extends this and
 * implements the means of delivery
 */
class NotificationUnit(
  val message: String,
  val channel: NotificationChannel
  ) extends Serializable {}
