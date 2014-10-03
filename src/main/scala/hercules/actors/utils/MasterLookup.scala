package hercules.actors.utils

import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.AddressFromURIString
import akka.actor.RootActorPath
import akka.contrib.pattern.ClusterClient
import akka.japi.Util.immutableSeq
import akka.actor.ActorRef
import com.typesafe.config.Config
import akka.actor.ActorSelection
import akka.actor.Props

/**
 * Provides a method to get a cluster client which point to the Master node
 */
trait MasterLookup {

  /**
   * Get the default cluster client by looking up the past to the system receptionist.
   *
   * @param system		in which to create the cluster client.
   * @param conf		A config which defines the path to the masters contact points.
   * @return A cluster client actor reference.
   */
  def getDefaultClusterClient(system: ActorSystem, conf: Config): ActorRef = {

    val initialContacts = immutableSeq(conf.getStringList("master.contact-points")).map {
      case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    system.actorOf(ClusterClient.props(initialContacts), "clusterClient")

  }

  /**
   * Load the default config from the application.conf file
   */
  def getDefaultConfig(): Config = {

    val hostname = InetAddress.getLocalHost().getHostName()

    val generalConfig = ConfigFactory.load()
    val conf =
      generalConfig.
        getConfig("remote.actors").
        withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostname")).
        withFallback(generalConfig)

    conf
  }

  /**
   * By calling this method one can look up a reference to the cluster receptionist.
   * This enables message passing to any actor which has registered to the cluster receptionist,
   * e.g. the master.
   *
   * @param system 				in which to create the cluster client actor
   * @param getConfig 			a function returning a Config.
   * 							Used to setup the client actor. Has a sensible default, can be overriden for tests.
   * @param getClusterClient    A function used to get the cluster client. Has a sensible default, can be overriden for tests.
   * @return a reference to a cluster client
   */
  def getMasterClusterClient(
    system: ActorSystem,
    getConfig: () => Config = getDefaultConfig,
    getClusterClient: (ActorSystem, Config) => ActorRef = getDefaultClusterClient): ActorRef = {

    val conf = getConfig()
    getClusterClient(system, conf)
  }

}