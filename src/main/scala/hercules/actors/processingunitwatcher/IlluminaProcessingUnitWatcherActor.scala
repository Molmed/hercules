package hercules.actors.processingunitwatcher

import hercules.config.processing.ProcessingUnitWatcherConfig
import akka.actor.Props
import hercules.config.processing.ProcessingUnitWatcherConfig
import hercules.entities.ProcessingUnit
import hercules.actors.HerculesActor
import hercules.protocols.HerculesMainProtocol
import akka.actor.ActorRef
import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.japi.Util.immutableSeq
import akka.actor.AddressFromURIString
import akka.actor.RootActorPath
import akka.contrib.pattern.ClusterClient
import hercules.actors.utils.MasterLookup

object IlluminaProcessingUnitWatcherActor extends MasterLookup {

  def startIlluminaProcessingUnitWatcherActor(): Unit = {
  
    val (clusterClient, system) = getMasterClusterClientAndSystem()
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

  context.system.actorOf(
    IlluminaProcessingUnitExecutorActor.props(),
    "IlluminaProcessingUnitExecutor")

  def receive = {
    case message: HerculesMainProtocol.FoundProcessingUnitMessage =>

  }
}