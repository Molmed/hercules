package hercules.actors.demultiplexing

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.japi.Util.immutableSeq
import akka.actor.Props
import akka.actor.AddressFromURIString
import akka.actor.RootActorPath
import akka.contrib.pattern.ClusterClient
import akka.actor.ActorRef
import akka.contrib.pattern.ClusterClient.SendToAll
import java.net.InetAddress

object IlluminaDemultiplexingActor {

  /**
   * Initiate all the stuff needed to start a IlluminaDemultiplexingActor
   * including initiating the system.
   */
  def startIlluminaDemultiplexingActor(): Unit = {

    val hostname = InetAddress.getLocalHost().getHostName()

    val conf = ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostname").
      withFallback(ConfigFactory.load("IlluminaDemultiplexingActor"))

    val system = ActorSystem("IlluminaDemultiplexingSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) ⇒ system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    val props = IlluminaDemultiplexingActor.props(clusterClient)
    system.actorOf(props, "demultiplexer")
  }

  /**
   * Create a new IlluminaDemultiplexingActor
   * @param clusterClient A reference to a cluster client thorough which the
   *                      actor will communicate with the rest of the cluster.
   */
  def props(clusterClient: ActorRef): Props = {
    Props(new IlluminaDemultiplexingActor(clusterClient))
  }
}

/**
 * Actors which demultiplex Illumina runfolders should communitate through
 * here. A concrete executor actor (such as the SisyphusDemultiplexingActor)
 * should do the actual work.
 * @param clusterClient A reference to a cluster client thorough which the
 *                      actor will communicate with the rest of the cluster.
 */
class IlluminaDemultiplexingActor(clusterClient: ActorRef) extends DemultiplexingActor {

  //@TODO Implement some real code here!
  
  clusterClient ! SendToAll("/user/master/active", "I'm alive")

  def receive = {
    case _ => log.info("IlluminaDemultiplexingActor got a message!")
  }

}