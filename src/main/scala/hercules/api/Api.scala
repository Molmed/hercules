package hercules.api

import akka.actor.ActorRefFactory
import akka.util.Timeout

import scala.concurrent.duration.Duration
import scala.util.DynamicVariable

import spray.routing.RouteConcatenation
import hercules.actors.api.RoutedHttpService
import hercules.api.services._

/**
 * The REST API layer. It exposes the REST services, but does not provide any
 * web server interface.<br/>
 * Notice that it requires to be mixed in with ``core.CoreActors``, which provides access
 * to the top-level actors that make up the system.
 * TODO document the api using e.g. spray-swagger: https://github.com/gettyimages/spray-swagger
 */
trait Api extends RouteConcatenation {
  this: CoreActors with Core =>

  private implicit val _ = system.dispatcher

  val statusService = new StatusService with BootedCore with CoreActors { def actorRefFactory = system }
  val demultiplexingService = new DemultiplexingService with BootedCore with CoreActors { def actorRefFactory = system }

  val rootService = system.actorOf(
    RoutedHttpService.props(
      demultiplexingService.route ~
        statusService.route
    ),
    "hercules-api-service")
}

