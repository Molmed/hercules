package hercules.actors.api

import akka.actor.{ ActorRef, Props }
import akka.util.Timeout

import scala.concurrent.{ duration, ExecutionContext }

import spray.routing.{ HttpService, HttpServiceActor, Route }
import spray.util.{ SprayActorLogging, LoggingContext }

import hercules.api.{ Api }
import hercules.api.services._

object RoutedHttpService {

  def props(route: Route) = {
    Props(new RoutedHttpService(route))
  }
}

/**
 * Allows you to construct Spray ``HttpService`` from a concatenation of routes; and wires in the error handler.
 * It also logs all internal server errors using ``SprayActorLogging``.
 *
 * @param route the (concatenated) route
 */
class RoutedHttpService(route: Route) extends HttpServiceActor
    with SprayActorLogging {

  def receive: Receive =
    runRoute(route)

}
