package hercules.api

import hercules.actors.api.RoutedHttpService
import spray.routing.RouteConcatenation

/**
 * The REST API layer. It exposes the REST services, but does not provide any
 * web server interface.
 * Notice that it requires to be mixed in with ``core.CoreActors``, which provides access
 * to the top-level actors that make up the system.
 */
trait Api extends RouteConcatenation {
  this: CoreActors with Core =>

  /**
   * Create an actor that will handle all requests. The routes of the services in the CoreActors trait are concatenated.
   */
  val rootService = system.actorOf(
    RoutedHttpService.props(
      services.map { _.route }.reduceLeft { _ ~ _ }
    ),
    "hercules-api-service")
}

