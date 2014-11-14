package hercules.api.services

import spray.routing.{ HttpService, Route }

trait HerculesService extends HttpService {
  def route: Route
}