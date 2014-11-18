package hercules.actors.api

import akka.actor.Props

import spray.routing.{ HttpServiceActor, Route }
import spray.util.SprayActorLogging

/**
 * The ``RoutedHttpService`` object
 */
object RoutedHttpService {

  /**
   * Provides props for creating a RoutedHttpService
   * @param route The route that will be used in the service to process requests
   * @return Props to create a RoutedHttpService
   */
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

  /**
   * The receive method passes incoming messages to the route
   */
  def receive: Receive =
    runRoute(route)

}
