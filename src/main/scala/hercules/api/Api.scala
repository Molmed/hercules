package hercules.api

import spray.routing.RouteConcatenation
import hercules.actors.api.RoutedHttpService
import hercules.api.services.DemultiplexingService
import hercules.api.services.StatusService

/**
 * The REST API layer. It exposes the REST services, but does not provide any
 * web server interface.<br/>
 * Notice that it requires to be mixed in with ``core.CoreActors``, which provides access
 * to the top-level actors that make up the system.
 * @TODO document the api using e.g. spray-swagger: https://github.com/gettyimages/spray-swagger
 */
trait Api extends RouteConcatenation {
  this: CoreActors with Core =>

  private implicit val _ = system.dispatcher

  val routes =
    new DemultiplexingService(cluster).route ~
      new StatusService(cluster).route

  val rootService = system.actorOf(RoutedHttpService.props(routes), "hercules-api-service")

}
