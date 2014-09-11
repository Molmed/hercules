package hercules.actors.notifiers

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

object EmailNotifierActor {

  /**
   * Initiate all the stuff needed to start a EmailNotifierActor
   * including initiating the system.
   */

  def startEmailNotifierActor(): Unit = {

    val hostname = InetAddress.getLocalHost().getHostName()

    val conf = ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostname").
      withFallback(ConfigFactory.load("EmailNotifierActor"))

    val system = ActorSystem("NotificationSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) ⇒ system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    val props = EmailNotifierActor.props(clusterClient)
    system.actorOf(props, "notifier")
  }

  /**
   * Create a new EmailNotifierActor
   * @param clusterClient A reference to a cluster client thorough which the
   *                      actor will communicate with the rest of the cluster.
   */
  def props(clusterClient: ActorRef): Props = {
    Props(new EmailNotifierActor(clusterClient))
  }
}

class EmailNotifierActor(clusterClient: ActorRef) extends NotifierActor {
  
  clusterClient ! SendToAll("/user/master/active", "I'm alive")

  def receive = {
    case _ => log.info("EmailNotifierActor got a message!")
  }

}