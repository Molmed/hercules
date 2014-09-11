package hercules.actors.processingunitwatcher

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.contrib.pattern.ClusterClient.SendToAll
import hercules.actors.utils.MasterLookup
import hercules.protocols.HerculesMainProtocol
import akka.actor.ActorSystem

object IlluminaProcessingUnitWatcherActor extends MasterLookup {

  def startIlluminaProcessingUnitWatcherActor(): Unit = {
  
    val (clusterClient, system) = getMasterClusterClientAndSystem("IlluminaProcessingUnitExecutorActor")
    val props = IlluminaProcessingUnitWatcherActor.props(clusterClient)
    system.actorOf(props, "IlluminaProcessingUnitWatcher")
  }

  def props(clusterClient: ActorRef): Props = {
    Props(new IlluminaProcessingUnitWatcherActor(clusterClient))
  }

}

/**
 * Base class for Actors which are watching for finished illumina runfolders.
 */
class IlluminaProcessingUnitWatcherActor(clusterClient: ActorRef)
    extends ProcessingUnitWatcherActor {
 
  context.actorOf(
    IlluminaProcessingUnitWatcherExecutorActor.props(),
    "IlluminaProcessingUnitExecutor")

  def receive = {
    case message: HerculesMainProtocol.FoundProcessingUnitMessage => {
      clusterClient ! SendToAll("/user/master/active", message)
    }
  }
}