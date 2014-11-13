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

/**
 *
 */
object IlluminaProcessingUnitWatcherActor extends MasterLookup {

  /**
   * Starts up a IlluminaProcessingUnitWatcherActo
   * 
   * @param system the system to start up in.
   * @param executor The executor to use. 
   * @param clusterClientCustomConfig Configuration for the cluster client
   * @param getClusterClient Start up the cluster client.
   * @return A reference to a IlluminaProcessingUnitWatcherActor
   */
  def startIlluminaProcessingUnitWatcherActor(
    system: ActorSystem = ActorSystem("IlluminaProcessingUnitWatcherSystem"),
    executor: Props = IlluminaProcessingUnitWatcherExecutorActor.props(),
    clusterClientCustomConfig: () => Config = getDefaultConfig,
    getClusterClient: (ActorSystem, Config) => ActorRef = getDefaultClusterClient): ActorRef = {

    val clusterClient = getMasterClusterClient(system, clusterClientCustomConfig, getClusterClient)
    val props = IlluminaProcessingUnitWatcherActor.props(clusterClient, executor)

    system.actorOf(props, "IlluminaProcessingUnitWatcher")
  }

  /**
   * Get a props to create a IlluminaProcessingUnitWatcherActor
   * 
   * @param clusterClient reference to the Master
   * @param executor the executor to use
   * @return a Props used to create a IlluminaProcessingUnitWatcherActor
   */
  def props(
    clusterClient: ActorRef,
    executor: Props = IlluminaProcessingUnitWatcherExecutorActor.props()): Props = {

    Props(new IlluminaProcessingUnitWatcherActor(
      clusterClient,
      executor))
  }

}

/**
 * IlluminaProcessingUnitWatcherActor watches for new processing units (runfolders)
 * to turn up. It will also periodically send requests to the master asking if
 * there are any processing units which should be forgotten (and thus reprocessed).
 *
 * @param clusterClient the cluster client though which one can contact the master
 * @param executor The executor which does the actual looking to the processing units.
 */
class IlluminaProcessingUnitWatcherActor(clusterClient: ActorRef, executor: Props)
    extends ProcessingUnitWatcherActor {

  import HerculesMainProtocol._
  import context.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val child = context.actorOf(
    executor,
    "IlluminaProcessingUnitExecutor")

  // Schedule a recurrent request for checking-in with the master if there
  // are any processing units which should be forgotten.
  val askForWork =
    context.system.scheduler.schedule(
      60.seconds,
      60.seconds,
      self,
      { RequestProcessingUnitMessageToForget })

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    askForWork.cancel()
  }

  def receive = LoggingReceive {

    // Pass a request for work up to master
    case RequestProcessingUnitMessageToForget =>
      clusterClient ! SendToAll("/user/master/active", RequestProcessingUnitMessageToForget)

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