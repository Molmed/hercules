package hercules.actors.demultiplexing

import java.io.File
import scala.concurrent.duration.DurationInt
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.routing.RoundRobinRouter
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import hercules.actors.utils.MasterLookup
import hercules.protocols.HerculesMainProtocol._
import akka.actor.ActorSystem
import com.typesafe.config.Config

object IlluminaDemultiplexingActor extends MasterLookup {

  /**
   * Initiate all the stuff needed to start a IlluminaDemultiplexingActor
   * including initiating the system.
   */
  def startIlluminaDemultiplexingActor(
    system: ActorSystem = ActorSystem("IlluminaDemultiplexingSystem"),
    executor: Props = SisyphusDemultiplexingExecutorActor.props(),
    clusterClientCustomConfig: () => Config = getDefaultConfig,
    getClusterClient: (ActorSystem, Config) => ActorRef = getDefaultClusterClient): ActorRef = {

    val clusterClient = getMasterClusterClient(system, clusterClientCustomConfig, getClusterClient)
    val props = IlluminaDemultiplexingActor.props(clusterClient, executor)
    system.actorOf(props, "demultiplexer")
  }

  /**
   * Create a new IlluminaDemultiplexingActor
   * @param clusterClient A reference to a cluster client thorough which the
   *                      actor will communicate with the rest of the cluster.
   * @param demultiplexingExecutor to use. Will default to the SisyphusDemultiplexingExecutorActor
   */
  def props(
    clusterClient: ActorRef,
    demultiplexingExecutor: Props = SisyphusDemultiplexingExecutorActor.props()): Props = {

    Props(new IlluminaDemultiplexingActor(clusterClient, demultiplexingExecutor))
  }
  
  object IlluminaDemultiplexingActorProtocol {
      case object Idle
      case object Busy
  }
}

/**
 * Actors which demultiplex Illumina runfolders should communitate through
 * here. A concrete executor actor (such as the SisyphusDemultiplexingActor)
 * should do the actual work.
 * @param clusterClient A reference to a cluster client thorough which the
 *                      actor will communicate with the rest of the cluster.
 * @param demultiplexingExecutor The executor to use
 */
class IlluminaDemultiplexingActor(
    clusterClient: ActorRef,
    demultiplexingExecutor: Props) extends DemultiplexingActor {

  implicit val timeout = Timeout(5.seconds)
  
  //@TODO Make the number of demultiplexing instances started configurable.
  val demultiplexingRouter =
    context.actorOf(
      demultiplexingExecutor.
        withRouter(RoundRobinRouter(nrOfInstances = 2)),
      "SisyphusDemultiplexingExecutor")

  import context.dispatcher

  def receive = {

    // Forward a request demultiplexing message to master
    case RequestDemultiplexingProcessingUnitMessage =>
      log.debug("Received a RequestDemultiplexingProcessingUnitMessage and passing it on to the master.")
      clusterClient ! SendToAll("/user/master/active",
        RequestDemultiplexingProcessingUnitMessage)

    case message: StartDemultiplexingProcessingUnitMessage => {

      log.debug("Received a StartDemultiplexingProcessingUnitMessage.")
      demultiplexingRouter.ask(message).pipeTo(sender)

    }

    case message: FinishedDemultiplexingProcessingUnitMessage =>
      log.debug("Got a FinishedDemultiplexingProcessingUnitMessage will forward it to the master.")
      clusterClient ! SendToAll("/user/master/active",
        message)

    case message: FailedDemultiplexingProcessingUnitMessage => {
      log.debug("Got a FailedDemultiplexingProcessingUnitMessage will forward it to the master.")
      clusterClient ! SendToAll("/user/master/active",
        message)
    }
  }

}