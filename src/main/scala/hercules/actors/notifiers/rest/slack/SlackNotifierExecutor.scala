package hercules.actors.notifiers.rest.slack

import akka.actor.Props
import hercules.actors.notifiers.NotificationExecutor
import hercules.actors.notifiers.rest.{ RestClient, RestNotifierExecutor }
import hercules.config.notification.SlackNotificationConfig
import hercules.protocols.HerculesMainProtocol.NotificationUnitMessage

import scala.concurrent.Future
import scalaj.http.{ HttpResponse, Http }

object SlackNotifierExecutor {
  /**
   * Used to create a SlackNotifierExecutor actor instance.
   * @param config to use
   * @return a Props for the SlackNotifierExecutor
   */
  def props(config: SlackNotificationConfig): Props = {
    Props(new SlackNotifierExecutor(config))
  }
}

/**
 * The SlackNotifierExecutor will pass on any message sent to it to the Slack channel defined by the config.
 * @param config to use.
 */
class SlackNotifierExecutor(config: SlackNotificationConfig) extends NotificationExecutor with RestNotifierExecutor with RestClient {

  /**
   * Any messages received will be end into the Slack channel.
   * @param message to be sent to Slack
   * @return a future containing info on if the http request was successful or not.
   */
  override def mapMessageToEndPoint(message: NotificationUnitMessage): Future[HttpResponse[String]] = {

    import context.dispatcher

    val requestResponse: Future[HttpResponse[String]] =
      Future {
        Http(config.slackEndPoint).
          postData(
            s"""payload={"channel": "#${config.slackChannel}", "username": "${config.slackUserName}",""" +
              s""" "text": "${message.unit.message}", "icon_emoji": "${config.iconEmoji}"}""").execute()
      }

    requestResponse
  }
}
