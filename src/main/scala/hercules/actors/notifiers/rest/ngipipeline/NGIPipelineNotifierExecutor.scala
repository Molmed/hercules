package hercules.actors.notifiers.rest.ngipipeline

import akka.actor.{ ActorLogging, Actor, Props }
import hercules.actors.notifiers.NotificationExecutor
import hercules.actors.notifiers.rest.{ RestClient, RestNotifierExecutor }
import hercules.config.notification.NGIPipelineNotificationConfig
import hercules.protocols.HerculesMainProtocol.{ FinishedDemultiplexingProcessingUnitMessage, NotificationUnitMessage }

import scala.concurrent.Future
import scalaj.http.{ HttpResponse, Http }

/**
 *
 * The executor which communicates with the NGI pipeline
 *
 * Created by johda411 on 2015-03-20.
 */
object NGIPipelineNotifierExecutor {

  /**
   * Used to create a SlackNotifierExecutor actor instance.
   * @param config to use
   * @return a Props for the SlackNotifierExecutor
   */
  def props(config: NGIPipelineNotificationConfig): Props = {
    Props(new NGIPipelineNotifierExecutor(config))
  }

}

/**
 *
 * The executor which communicates with the NGI pipeline - this will only work with
 * FinishedDemultiplexingProcessingUnitMessage.
 *
 * TODO Implement mechanism to check if this is a runfolder that should actually be
 * processed by the NGI pipeline
 *
 *
 * Created by johda411 on 2015-03-20.
 */
class NGIPipelineNotifierExecutor(config: NGIPipelineNotificationConfig) extends NotificationExecutor with RestNotifierExecutor with RestClient {

  this: Actor with ActorLogging with RestClient =>

  private def triggerNGIPipeline(runfolder: String): HttpResponse[String] = {
    Http(s"http://${config.host}:${config.port}/flowcell_analysis/$runfolder").execute()
  }

  /**
   * Any messages received will be end into the Slack channel.
   * @param message to be sent to Slack
   * @return a future containing info on if the http request was successful or not.
   */
  override def mapMessageToEndPoint(message: NotificationUnitMessage): Future[HttpResponse[String]] = {

    import context.dispatcher

    val requestResponse: Future[HttpResponse[String]] =
      Future {

        val finishedMessage = message.unit.originalMessage.getOrElse(
          throw new IllegalArgumentException("If this exception is thrown it means that the pre-filtering " +
            "of the message channel in the NGPPipelineNotfierActor has not worked as expected. The message was: " +
            message))

        // Suppressing compiler warnings here since anything but a FinishedDemultiplexingProcessingUnitMessage
        // should be dropped here.
        (finishedMessage: @unchecked) match {
          case FinishedDemultiplexingProcessingUnitMessage(unit) => triggerNGIPipeline(unit.name)
        }
      }

    requestResponse
  }

}
