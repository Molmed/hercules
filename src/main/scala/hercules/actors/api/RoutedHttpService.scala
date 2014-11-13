package hercules.actors.api

import akka.actor.{ ActorRef, Props }
import akka.util.Timeout

import com.gettyimages.spray.swagger._
import com.wordnik.swagger.model.ApiInfo

import scala.concurrent.{ duration, ExecutionContext }
import scala.reflect.runtime.universe._

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
    runRoute(
      route ~
        swaggerService.routes ~
        swaggerService.getRoute)

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[StatusService])
    override def apiVersion = "2.0"
    override def baseUrl = "http://localhost:8001"
    override def docsPath = "api-docs"
    override def actorRefFactory = context
    override def apiInfo = Some(
      new ApiInfo(
        title = "Hercules REST API",
        description = "REST API for controlling key Hercules operations.",
        termsOfServiceUrl = "https://github.com/Molmed/hercules",
        contact = "Pontus Larsson @b97pla",
        license = "MIT",
        licenseUrl = "https://github.com/Molmed/hercules"))

    def getRoute =
      get {
        pathPrefix("") {
          pathEndOrSingleSlash {
            getFromResource("docs/index.html")
          }
        } ~
          getFromResourceDirectory("docs")
      }
  }
}
