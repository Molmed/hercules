package hercules.actors.demultiplexing

import java.io.File
import scala.concurrent.duration.DurationInt
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.routing.RoundRobinRouter
import hercules.actors.utils.MasterLookup
import hercules.protocols.HerculesMainProtocol
import hercules.protocols.HerculesMainProtocol.Acknowledge
import hercules.protocols.HerculesMainProtocol.FinishedDemultiplexingProcessingUnitMessage
import hercules.protocols.HerculesMainProtocol.Reject
import hercules.protocols.HerculesMainProtocol.StringMessage
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

  import HerculesMainProtocol._

  //@TODO Make the number of demultiplexing instances started configurable.
  val demultiplexingRouter =
    context.actorOf(
      demultiplexingExecutor.
        withRouter(RoundRobinRouter(nrOfInstances = 2)),
      "SisyphusDemultiplexingExecutor")

  import context.dispatcher

  //@TODO Make request new work period configurable.
  // Request new work periodically
  val requestWork =
    context.system.scheduler.schedule(10.seconds, 10.seconds, self, {
      HerculesMainProtocol.RequestDemultiplexingProcessingUnitMessage
    })

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    requestWork.cancel()
  }

  def receive = {

    case RequestDemultiplexingProcessingUnitMessage =>
      log.info("Received a RequestDemultiplexingProcessingUnitMessage and passing it on to the master.")
      clusterClient ! SendToAll("/user/master/active",
        HerculesMainProtocol.RequestDemultiplexingProcessingUnitMessage)

    case message: StartDemultiplexingProcessingUnitMessage => {

      //@TODO It is probably reasonable to have some other mechanism than checking if it
      // can spot the file if it can spot the file or not. But for now, this will have to do.
      log.info("Received a StartDemultiplexingProcessingUnitMessage.")

      val pathToTheRunfolder = new File(message.unit.uri)
      if (pathToTheRunfolder.exists()) {
        log.info("Found the runfolder and will acknowlede message to sender: " + sender)
        sender ! Acknowledge
        demultiplexingRouter ! message
      } else {
        log.info("Didn't find the runfolder. Will REJECT: " + message.unit)
        sender ! Reject
      }
    }

    case message: FinishedDemultiplexingProcessingUnitMessage =>
      log.info("Got a FinishedDemultiplexingProcessingUnitMessage will forward it to the master.")
      clusterClient ! SendToAll("/user/master/active",
        message)
  }

}