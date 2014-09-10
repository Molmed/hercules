package hercules.actors.utils

import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.AddressFromURIString
import akka.actor.RootActorPath
import akka.contrib.pattern.ClusterClient
import akka.japi.Util.immutableSeq
import akka.actor.ActorRef


/**
 * Provides a method to get a cluster client which point to the Master node
 */
trait MasterLookup {
  
  /**
   * Looks up the cluster receptionist for the master
   */
  def getMasterClusterClientAndSystem(): (ActorRef, ActorSystem) = {
    val systemIdentifier = "ClusterSystem"
      
    val hostname = InetAddress.getLocalHost().getHostName()
    val conf = ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostname").
      withFallback(ConfigFactory.load())

    val system = ActorSystem(systemIdentifier, conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) ⇒ system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    (system.actorOf(ClusterClient.props(initialContacts), "clusterClient"), system)
  }

}