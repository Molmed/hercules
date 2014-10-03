package hercules.actors.api

import akka.actor.Props
import spray.routing.{HttpServiceActor, Route}
import spray.util.{SprayActorLogging, LoggingContext}

object RoutedHttpService {
  
  def props(route: Route): Props = {
    Props(new RoutedHttpService(route))
  }
}
/**
 * Allows you to construct Spray ``HttpService`` from a concatenation of routes; and wires in the error handler.
 * It also logs all internal server errors using ``SprayActorLogging``.
 *
 * @param route the (concatenated) route
 */
class RoutedHttpService(route: Route) extends HttpServiceActor with SprayActorLogging {

  def receive: Receive =
    runRoute(route)

}

