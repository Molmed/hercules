package hercules.actors.notifiers.email

import hercules.entities.notification.EmailNotificationUnit
import hercules.protocols.NotificationChannelProtocol.NotificationChannel

import scala.concurrent.Future

/**
 * Created by johda411 on 2015-03-17.
 */
object FakeEmailNotificationObjects {

  /**
   * A FakeEmailNotificationUnit class to mock sending emails
   *
   */
  class FakeEmailNotificationUnit(
      override val message: String,
      override val channel: NotificationChannel,
      override val attempts: Int,
      val exception: Option[Exception]) extends EmailNotificationUnit(message, channel, attempts) {

    override def sendNotification(
      recipients: Seq[String],
      sender: String,
      prefix: String,
      smtpHost: String,
      smtpPort: Int): Future[Unit] = {
      if (exception.isEmpty) Future.successful()
      else Future.failed(exception.get)
    }
  }
}
