package hercules.protocols

object NotificationChannelProtocol {
  sealed trait NotificationChannel

  case object Progress extends NotificationChannel
  case object Info extends NotificationChannel
  case object Warning extends NotificationChannel
  case object Critical extends NotificationChannel
}