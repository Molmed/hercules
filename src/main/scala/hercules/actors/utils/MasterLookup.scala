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
   * @TODO Write documentation!
   *
   * @param system
   * @param conf
   * @return
   */
  def getDefaultClusterClient(system: ActorSystem, conf: Config): ActorRef = {

    val initialContacts = immutableSeq(conf.getStringList("master.contact-points")).map {
      case AddressFromURIString(addr) ⇒ system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
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
   * @TODO Write documentation!
   *
   * @param system
   * @param getConfig
   * @param getClusterClient
   * @return
   */
  def getMasterClusterClient(
    system: ActorSystem,
    getConfig: () => Config = getDefaultConfig,
    getClusterClient: (ActorSystem, Config) => ActorRef = getDefaultClusterClient) = {

    val conf = getConfig()
    getClusterClient(system, conf)
  }

}