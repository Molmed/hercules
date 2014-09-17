package hercules.actors.processingunitwatcher

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.contrib.pattern.ClusterClient.SendToAll
import hercules.actors.utils.MasterLookup
import hercules.protocols.HerculesMainProtocol
import akka.actor.ActorSystem
import com.typesafe.config.Config

object IlluminaProcessingUnitWatcherActor extends MasterLookup {

  def startIlluminaProcessingUnitWatcherActor(customConfig: () => Config = getDefaultConfig): ActorRef = {

    val config = customConfig
    val (clusterClient, system) = getMasterClusterClientAndSystem(config)
    val props = IlluminaProcessingUnitWatcherActor.props(clusterClient)

    system.actorOf(props, "IlluminaProcessingUnitWatcher")
  }

  def props(
    clusterClient: ActorRef,
    executor: Props = IlluminaProcessingUnitWatcherExecutorActor.props()): Props = {

    Props(new IlluminaProcessingUnitWatcherActor(
      clusterClient,
      executor))
  }

}

/**
 * Base class for Actors which are watching for finished illumina runfolders.
 */
class IlluminaProcessingUnitWatcherActor(clusterClient: ActorRef, executor: Props)
    extends ProcessingUnitWatcherActor {

  context.actorOf(
    executor,
    "IlluminaProcessingUnitExecutor")

  def receive = {
    case message: HerculesMainProtocol.FoundProcessingUnitMessage => {
      log.info("Got a FoundProcessingUnitMessage")
      clusterClient ! SendToAll("/user/master/active", message)
    }
  }
}