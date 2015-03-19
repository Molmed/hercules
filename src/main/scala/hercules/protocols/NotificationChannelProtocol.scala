package hercules.protocols

/**
 * Protocol for the notification channel, indicating which level the notification
 * is on.
 */
object NotificationChannelProtocol {

  /**
   * TODO Move this to some more general place! /JD 20150317
   *
   * Converts a string to the correct notification channel
   *
   * @param str to convert to a NotificationChannel
   * @return A NotificationChannel
   */
  def stringToChannel(str: String): NotificationChannel = str match {
    case "progress" => Progress
    case "info"     => Info
    case "warning"  => Warning
    case "critical" => Critical
  }

  sealed trait NotificationChannel

  case object Progress extends NotificationChannel
  case object Info extends NotificationChannel
  case object Warning extends NotificationChannel
  case object Critical extends NotificationChannel
}