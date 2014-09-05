package hercules.actors.masters

import akka.actor.ActorContext
import akka.actor.Props
import hercules.protocols.HerculesMainProtocol
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.ClusterReceptionistExtension
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import scala.collection.JavaConversions._
import akka.actor.PoisonPill
import akka.contrib.pattern.ClusterSingletonManager
import java.net.InetAddress

object SisyphusMasterActor {

  /**
   * Initiate all the stuff needed to start a SisyphusMasterActor
   * including initiating the system.
   */
  def startSisyphusMasterActor(): Unit = {
    val role = "master"

    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", conf)

    val primarySeedNode = conf.getStringList("akka.cluster.seed-nodes").head

    system.actorOf(
      ClusterSingletonManager.props(
        SisyphusMasterActor.props(),
        "active",
        PoisonPill,
        Some(role)),
      role)
  }

  /**
   * Create a new SisyphusMasterActor
   */
  def props(): Props = Props(new SisyphusMasterActor())
}

/**
 * Defines the logic for running the Sisyphus workflow
 * 
 * The other actors in the system will register to the master,
 * and request work from it. If the master has work for the actor it will
 * send it.
 */
class SisyphusMasterActor extends HerculesMasterActor {

  // The master will register it self to the cluster receptionist.
  ClusterReceptionistExtension(context.system).registerService(self)

    //@TODO Implement some real code here!
  
  def receive = {
    case HerculesMainProtocol.StringMessage(s) => log.info(s"I got this message: $s")
    case _                                     => log.info("Sisyphus master got a message!")
  }

}