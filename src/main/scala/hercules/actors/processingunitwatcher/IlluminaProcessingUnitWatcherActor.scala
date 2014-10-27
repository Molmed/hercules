package hercules.actors.processingunitwatcher

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import akka.util.Timeout
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.event.LoggingReceive
import hercules.actors.utils.MasterLookup
import hercules.protocols.HerculesMainProtocol
import akka.actor.ActorSystem
import com.typesafe.config.Config
import akka.actor.ActorSelection

object IlluminaProcessingUnitWatcherActor extends MasterLookup {

  def startIlluminaProcessingUnitWatcherActor(
    system: ActorSystem = ActorSystem("IlluminaProcessingUnitWatcherSystem"),
    executor: Props = IlluminaProcessingUnitWatcherExecutorActor.props(),
    clusterClientCustomConfig: () => Config = getDefaultConfig,
    getClusterClient: (ActorSystem, Config) => ActorRef = getDefaultClusterClient): ActorRef = {

    val clusterClient = getMasterClusterClient(system, clusterClientCustomConfig, getClusterClient)
    val props = IlluminaProcessingUnitWatcherActor.props(clusterClient, executor)

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

  import HerculesMainProtocol._
  import context.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val child = context.actorOf(
    executor,
    "IlluminaProcessingUnitExecutor")

  // Schedule a recurrent request for work
  val askForWork =
    context.system.scheduler.schedule(
      60.seconds,
      60.seconds,
      self,
      { RequestProcessingUnitMessage })

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    askForWork.cancel()
  }

  def receive = LoggingReceive {
    // Pass a request for work up to master
    case RequestProcessingUnitMessage =>
      clusterClient ! SendToAll("/user/master/active", RequestProcessingUnitMessage)
    case message: FoundProcessingUnitMessage => {
      log.debug("Got a FoundProcessingUnitMessage")
      clusterClient ! SendToAll("/user/master/active", message)
    }
    case message: ForgetProcessingUnitMessage => {
      val s = sender
      log.debug("Got a ForgetProcessingUnitMessage")
      ask(child, message).map {
        SendToAll("/user/master/active", _)
      }.pipeTo(clusterClient)
    }
  }
}