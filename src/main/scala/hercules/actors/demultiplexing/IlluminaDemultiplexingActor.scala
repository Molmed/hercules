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
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.entities.ProcessingUnit

/**
 * Provides factory methods for creating a IlluminaDemultiplexingActor
 */
object IlluminaDemultiplexingActor extends MasterLookup {

  /**
   * Initiate all the stuff needed to start a IlluminaDemultiplexingActor
   * including initiating the system.
   * @param system A actor system to initiate. Will create one by default.
   * @param executor The executor to use. Will default to the
   * 				 SisyphusDemultiplexingExecutorActor
   * @param clusterClientCustomConfig function to get a Config
   * @param getClusterClient The cluster client used to contact the master
   * @return A ActorRef to a new IlluminaDemultiplexingActor
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
 *
 * It create executors as necessary (up to a set maximum number). It will switch
 * its behavior depending on if all executors are busy or not.
 *
 * It's communication with the master is setup in such a way that it will
 * periodically request more work (provided that it's not busy), and then report
 * back to the master once the work is done.
 *
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

  //@TODO Make configurable
  var idleExecutorInstances: Set[ActorRef] =
    (1 to maximumNbrOfExectorInstances).map(
      _ => context.actorOf(demultiplexingExecutor)).
      toSet

  var runningExecutorInstances: Map[ProcessingUnit, ActorRef] = Map()

  // Schedule a recurrent request for work when idle
  import context.dispatcher
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

  /**
   * Will change the behavior depending on if the maximum number of jobs is
   * running or not.
   */
  def switchBehavior(): Unit = {
    val nbrOfIdleInstances = idleExecutorInstances.size
    if (nbrOfIdleInstances > 0) {
      log.debug(s"Has $nbrOfIdleInstances idle instances of " +
        s"max: $maximumNbrOfExectorInstances will accept more work.")
      context.become(canAcceptWork)
    } else {
      log.debug(s"Has $nbrOfIdleInstances idle instances of " +
        s"max: $maximumNbrOfExectorInstances will reject more work.")
      context.become(cannotAcceptWork)
    }
  }

  /**
   * Will default to canAcceptWork on start up.
   * @see akka.actor.Actor#receive()
   */
  def receive = canAcceptWork

  /**
   * To be activated when all executors are busy.
   * @return Receive partial function.
   */
  def cannotAcceptWork: Receive = LoggingReceive {

    // Forward a request demultiplexing message to master if we want more work!
    case RequestDemultiplexingProcessingUnitMessage =>
      log.debug("Right now all executors are busy. Won't request more work.")

    case message: StartDemultiplexingProcessingUnitMessage => {
      log.debug("Right now all executors are busy. Cannot start any more work.")
      sender ! Reject(reason = Some("All exectutors are currently busy. Try agin later."))
    }

    case message @ (_: FinishedDemultiplexingProcessingUnitMessage | _: FailedDemultiplexingProcessingUnitMessage) => {
      // These messages are handled in the same way regardless of the behavior, so avoid code duplication
      canAcceptWork(message)
    }
  }

  /**
   * To be activated when all executors are not busy.
   * @return Receive partial function.
   */
  def canAcceptWork: Receive = LoggingReceive {

    // Forward a request demultiplexing message to master if we want more work!
    case RequestDemultiplexingProcessingUnitMessage =>
      clusterClient ! SendToAll("/user/master/active",
        RequestDemultiplexingProcessingUnitMessage)

    case message: StartDemultiplexingProcessingUnitMessage => {
      val originalSender = sender

      val availableExecutor =
        idleExecutorInstances.headOption.
          getOrElse(
            throw new Exception("Something went wrong. Couldn't find idle instance though I was available for work."))

      (availableExecutor ? message).map {
        case Acknowledge =>
          idleExecutorInstances = idleExecutorInstances - availableExecutor
          runningExecutorInstances += message.unit -> availableExecutor
          switchBehavior()
          Acknowledge
        case rejectMessage: Reject =>
          log.debug("Executor rejected work.")
          switchBehavior()
          rejectMessage
      }.pipeTo(originalSender)
    }

    case message: FinishedDemultiplexingProcessingUnitMessage => {
      val finishedActor = runningExecutorInstances(message.unit)
      runningExecutorInstances = runningExecutorInstances - message.unit
      idleExecutorInstances = idleExecutorInstances + finishedActor
      switchBehavior()
      log.debug(s"Finished demultiplexing ${message.unit}")
      clusterClient ! SendToAll("/user/master/active", message)
    }

    case message: FailedDemultiplexingProcessingUnitMessage => {
      notice.critical(s"Demultiplexing failed for unit ${message.unit.name}, reason: ${message.reason}")
      log.warning(s"Demultiplexing failed for unit ${message.unit.name}, reason: ${message.reason}")
      val failingActor = runningExecutorInstances(message.unit)
      runningExecutorInstances = runningExecutorInstances - message.unit
      idleExecutorInstances = idleExecutorInstances + failingActor
      switchBehavior()
      clusterClient ! SendToAll("/user/master/active", message)
    }
  }

}