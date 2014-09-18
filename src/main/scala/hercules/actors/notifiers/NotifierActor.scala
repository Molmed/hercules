package hercules.actors.notifiers

import hercules.actors.HerculesActor
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import hercules.actors.utils.MasterLookup
import akka.actor.Props

/**
 * Send notifications of events (e.g. email them or push cards around on a
 * trello board).
 */
trait NotifierActor extends Actor with ActorLogging {}
trait ActorFactory extends MasterLookup {
  def props(client: ActorRef): Props 
  def startInstance(name: String): ActorRef = {
    val (clusterClient, system) = getMasterClusterClientAndSystem()
    system.actorOf(
      props(clusterClient), 
      name
    )
  }
}
  