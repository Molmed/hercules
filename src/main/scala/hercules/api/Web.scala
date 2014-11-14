package hercules.api

import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

/**
 * Provides the web server (spray-can) for the REST api in ``Api``, using the actor system
 * defined in ``Core``.
 */
trait Web {
  this: Api with CoreActors with Core =>

  implicit val timeout = Timeout(5.seconds)

  // TODO Read ip and port from configuration
  IO(Http)(system) ? Http.Bind(rootService, "0.0.0.0", port = 8001)

}
