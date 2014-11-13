package hercules.api

import akka.actor.{ Props, ActorSystem }
import akka.util.Timeout

import scala.concurrent.duration.Duration

/**
 * Core is type containing the ``system: ActorSystem`` member. This enables us to use it in our
 * apps as well as in our tests.
 */
trait Core {

  implicit def system: ActorSystem
  implicit val timeout: Timeout

}

/**
 * This trait implements ``Core`` by starting the required ``ActorSystem`` and registering the
 * termination handler to stop the system when the JVM exits.
 */
trait BootedCore extends Core {

  /**
   * Construct the ActorSystem we will use in our application
   */
  implicit lazy val system = ActorSystem("hercules-rest-api")
  implicit lazy val timeout = Timeout(Duration(5, "seconds"))

  /**
   * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
   */
  sys.addShutdownHook(system.shutdown())

}
