package hercules.actors.notifiers

import akka.actor.Props
import akka.actor.ActorRef
import hercules.protocols.HerculesMainProtocol
import hercules.actors.HerculesActor

object TestActor extends ActorFactory {

  /**
   * Create a new TestActor
   * @param clusterClient A reference to a cluster client thorough which the
   *                      actor will communicate with the rest of the cluster.
   */
  def props(clusterClient: ActorRef): Props = {
    Props(new TestActor(clusterClient))
  }
  
}

class TestActor (
  clusterClient: ActorRef) extends HerculesActor with Notifier {
    notice.info("Info: TestActor is launched")
    notice.warning("Warning: TestActor is launched")
    notice.progress("Progress: TestActor is launched")
    notice.critical("Critical: TestActor is launched")
  def receive = {
    case _ => println
  }
}
