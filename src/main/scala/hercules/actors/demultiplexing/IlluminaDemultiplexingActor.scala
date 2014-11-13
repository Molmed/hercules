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
import akka.event.LoggingReceive
import hercules.actors.utils.MasterLookup
import hercules.protocols.HerculesMainProtocol._
import akka.actor.ActorSystem
import com.typesafe.config.Config
import scala.concurrent.duration._
import akka.event.LoggingReceive
import scala.util.Random

/**
 * Provides factory methods for creating a IlluminaDemultiplexingActor
 */
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
    system.actorOf(props, "demultiplexer-" + Random.nextInt())
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
 * Actors which demultiplexes Illumina runfolders should communicate through
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

  /**
   * Will change the behavior depending on if the maximum number of jobs is
   * running or not.
   */
  def switchBehavior(incrementOfRunningInstances: Int): Unit = {

    runningExecutorInstances += incrementOfRunningInstances
    if (runningExecutorInstances < 0)
      runningExecutorInstances = 0

    require(
      runningExecutorInstances >= 0 &&
        !(runningExecutorInstances > maximumNbrOfExectorInstances),
      s"Not allowed value for number of $runningExecutorInstances. " +
        s"Max was: $maximumNbrOfExectorInstances")

    if (runningExecutorInstances < maximumNbrOfExectorInstances) {
      log.debug(s"Has $runningExecutorInstances running instances of " +
        s"max: $maximumNbrOfExectorInstances will accept more work.")

      context.become(canAcceptWork)
    } else {
      log.debug(s"Has $runningExecutorInstances running instances of " +
        s"max: $maximumNbrOfExectorInstances will reject more work.")
      context.become(cannotAcceptWork)
    }
  }

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
      // These messages are handled in the same way regardless of the behavior, so avoid code duplication
      canAcceptWork(message)
    }
  }

  def canAcceptWork: Receive = LoggingReceive {

    // Forward a request demultiplexing message to master if we want more work!
    case RequestDemultiplexingProcessingUnitMessage =>
      clusterClient ! SendToAll("/user/master/active",
        RequestDemultiplexingProcessingUnitMessage)

    case message: StartDemultiplexingProcessingUnitMessage => {
      val originalSender = sender

      (demultiplexingRouter ? message).map {
        case Acknowledge =>
          switchBehavior(1)
          Acknowledge
        case Reject =>
          log.debug("Executor rejected work.")
          switchBehavior(-1)
          Reject
      }.pipeTo(originalSender)
    }

    case message: FinishedDemultiplexingProcessingUnitMessage => {
      switchBehavior(-1)
      clusterClient ! SendToAll("/user/master/active", message)
    }

    case message: FailedDemultiplexingProcessingUnitMessage => {
      notice.critical(s"Demultiplexing failed for unit $message.unit.name, reason: $message.reason")
      switchBehavior(-1)
      clusterClient ! SendToAll("/user/master/active", message)
    }
  }

}