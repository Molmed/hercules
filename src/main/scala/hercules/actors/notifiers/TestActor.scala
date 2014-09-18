package hercules.actors.notifiers

import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.ActorSystem
import hercules.actors.HerculesActor
import hercules.actors.utils.MasterLookup

object TestActor extends MasterLookup {
  
  def startInstance(): ActorRef = {
    val system = ActorSystem("TestActorSystem")
    val clusterClient = getMasterClusterClient(system)
    system.actorOf(
      props(clusterClient), 
      "TestActor"
    )
  }

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
  clusterClient: ActorRef) extends HerculesActor {
    notice.info("Info: TestActor is launched")
    notice.warning("Warning: TestActor is launched")
    notice.progress("Progress: TestActor is launched")
    notice.critical("Critical: TestActor is launched")
  def receive = {
    case _ => println
  }
}
