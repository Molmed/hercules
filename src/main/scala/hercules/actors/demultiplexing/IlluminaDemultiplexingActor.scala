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
import scala.concurrent.duration._
import akka.event.LoggingReceive

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
    demultiplexingExecutor: Props,
    requestWorkInterval: FiniteDuration = 60.seconds) extends DemultiplexingActor {

  //@TODO Make configurable
  implicit val timeout = Timeout(10.seconds)
  val maximumNbrOfExectorInstances = 2

  var runningExecutorInstances = 0

  import context.dispatcher

  // Schedule a recurrent request for work when idle
  val requestWork =
    context.system.scheduler.schedule(
      requestWorkInterval,
      requestWorkInterval,
      self,
      { RequestDemultiplexingProcessingUnitMessage })

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    requestWork.cancel()
  }

  //@TODO Make the number of demultiplexing instances started configurable.
  val demultiplexingRouter =
    context.actorOf(
      demultiplexingExecutor.
        withRouter(RoundRobinRouter(nrOfInstances = maximumNbrOfExectorInstances)),
      "SisyphusDemultiplexingExecutor")

  import context.dispatcher

  def receive = canAcceptWork

  def cannotAcceptWork: Receive = LoggingReceive {
    // Forward a request demultiplexing message to master if we want more work!
    case RequestDemultiplexingProcessingUnitMessage =>
      log.debug("Right now all executors are busy. Won't request more work.")

    case message: StartDemultiplexingProcessingUnitMessage => {
      log.debug("Right now all executors are busy. Cannot start any more work.")
      sender ! Reject
    }

    case message @ (_: FinishedDemultiplexingProcessingUnitMessage | _: FailedDemultiplexingProcessingUnitMessage) => {
      runningExecutorInstances -= 1
      if (runningExecutorInstances < maximumNbrOfExectorInstances)
        context.become(canAcceptWork)
      clusterClient ! SendToAll("/user/master/active", message)
    }
  }

  def canAcceptWork: Receive = LoggingReceive {

    // Forward a request demultiplexing message to master if we want more work!
    case RequestDemultiplexingProcessingUnitMessage =>
      clusterClient ! SendToAll("/user/master/active",
        RequestDemultiplexingProcessingUnitMessage)

    case message: StartDemultiplexingProcessingUnitMessage => {
      val originalSender = sender

      log.debug(s"runningExecutorInstances=$runningExecutorInstances")
      runningExecutorInstances += 1

      if (runningExecutorInstances > maximumNbrOfExectorInstances) {
        log.debug("Has gotten enough work - will become cannotAcceptWork.")
        context.become(cannotAcceptWork)
      }

      (demultiplexingRouter ? message).pipeTo(originalSender)
    }

    case message: FinishedDemultiplexingProcessingUnitMessage => {
      runningExecutorInstances -= 1
      clusterClient ! SendToAll("/user/master/active", message)
    }

    case message: FailedDemultiplexingProcessingUnitMessage => {
      runningExecutorInstances -= 1
      clusterClient ! SendToAll("/user/master/active",
        message)
    }
  }

}