package hercules.api.services

import spray.routing.{ HttpService, Route }

/**
 * The base trait for REST services in the Hercules API.
 * All services should extend this and provide an implementation of the route method.
 */
trait HerculesService extends HttpService {
  def route: Route
}