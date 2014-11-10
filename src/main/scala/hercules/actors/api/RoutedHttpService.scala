package hercules.actors.api

import akka.actor.{ ActorRef, Props }
import akka.util.Timeout

import scala.concurrent.ExecutionContext

import spray.routing.{ HttpServiceActor, Route }
import spray.util.{ SprayActorLogging, LoggingContext }

import hercules.api.Api
import hercules.api.services._

object RoutedHttpService {

  def props(
    cluster: ActorRef,
    timeout: Timeout,
    executionContext: ExecutionContext): Props = {
    Props(new RoutedHttpService()(cluster, timeout, executionContext))
  }
}
/**
 * Allows you to construct Spray ``HttpService`` from a concatenation of routes; and wires in the error handler.
 * It also logs all internal server errors using ``SprayActorLogging``.
 *
 * @param route the (concatenated) route
 */
class RoutedHttpService()(
  implicit val cluster: ActorRef,
  implicit val timeout: Timeout,
  implicit val executionContext: ExecutionContext) extends HttpServiceActor
    with SprayActorLogging
    //    with StatusService
    with DemultiplexingService {

  def receive: Receive =
    runRoute(route)

}

