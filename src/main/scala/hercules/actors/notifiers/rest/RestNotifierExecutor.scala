package hercules.actors.notifiers.rest

import akka.actor.{ ActorLogging, Actor }
import akka.event.LoggingReceive
import hercules.protocols.HerculesMainProtocol.{ FailedNotificationUnitMessage, SentNotificationUnitMessage, SendNotificationUnitMessage }

import scala.util.{ Failure, Success }

/**
 * Mixin to implementing class e.g. a SlackNotifierExecutorActor to setup a actor
 * which will then map messages onto different end point using the RESTClient.
 * Created by johda411 on 2015-03-18.
 */
trait RestNotifierExecutor {
  this: Actor with ActorLogging with RestClient =>

  /**
   * Handles incoming messages. Escalates failure to the NotifierActor, which will handle
   * any retries.
   */
  def receive = LoggingReceive {
    case message: SendNotificationUnitMessage => {

      val parent = sender
      val requestResponse = mapMessageToEndPoint(message)

      import context.dispatcher

      requestResponse.onComplete {
        // Since the future only checks if we completed without throwing exceptions
        // we need to check if it was actually a true success.
        case Success(httpResponse) => {
          if (httpResponse.isError)
            parent ! FailedNotificationUnitMessage(message.unit, reason = httpResponse.statusLine)
          else
            parent ! SentNotificationUnitMessage(message.unit)
        }
        case Failure(failure) =>
          parent ! FailedNotificationUnitMessage(message.unit, reason = failure.getMessage())
      }
    }
  }

}
