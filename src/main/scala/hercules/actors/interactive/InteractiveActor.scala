package hercules.actors.interactive

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.japi.Util.immutableSeq
import akka.actor.Props
import akka.actor.AddressFromURIString
import akka.actor.RootActorPath
import akka.contrib.pattern.ClusterClient
import akka.actor.ActorRef
import java.net.InetAddress
import hercules.actors.utils.MasterLookup
import akka.routing.RoundRobinRouter
import scala.concurrent.duration._
import hercules.protocols.HerculesMainProtocol
import java.io.File
import hercules.actors.HerculesActor
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.actor.PoisonPill

object InteractiveActor extends MasterLookup {

  /**
   * Initiate all the stuff needed to start a InteractiveActor
   * including initiating the system.
   * @param command A string specifying which command to pass to Master
   * @param unitName Unit to do something with
   */
  def startInteractive(command: String, unitName: String): Unit = {

    val system = ActorSystem("IlluminaDemultiplexingActor")
    val clusterClient = getMasterClusterClient(system)
    val props = InteractiveActor.props(clusterClient, command, unitName)
    system.actorOf(props, "interactive")
  }

  /**
   * Create a new Interactive
   * @param clusterClient A reference to a cluster client thorough which the
   *                      actor will communicate with the rest of the cluster.
   * @param command A string specifying which command to pass to Master
   * @param unitName Unit to do something with
   */
  def props(clusterClient: ActorRef, command: String, unitName: String): Props = {
    Props(new InteractiveActor(clusterClient, command, unitName))
  }
}

/**
 * Actors which demultiplex Illumina runfolders should communitate through
 * here. A concrete executor actor (such as the SisyphusDemultiplexingActor)
 * should do the actual work.
 * @param clusterClient A reference to a cluster client thorough which the
 *                      actor will communicate with the rest of the cluster.
 * @param command A string specifying which command to pass to Master
 */
class InteractiveActor(clusterClient: ActorRef, command: String, unitName: String) extends HerculesActor {

  import HerculesMainProtocol._

  case class SendToMasterMessage

  import context.dispatcher

  // @TODO
  // Wait for a while before trying to send message, to make sure that the 
  // contact to the actor system is up. Maybe there is some nicer solution to
  // this, but for now this hack will have to be good enough!
  context.system.scheduler.scheduleOnce(3.seconds, self, {
    SendToMasterMessage
  })

  def receive = {
    case SendToMasterMessage => {

      log.info("Time to send a message to the master!")

      command match {
        case "restart" => {

          log.info("I'm trying to restart: " + unitName)

          import context.dispatcher
          implicit val timeout = Timeout(5 seconds)

          (clusterClient ?
            SendToAll(
              "/user/master/active",
              HerculesMainProtocol.RestartDemultiplexingProcessingUnitMessage(unitName))).
              map {
                case Acknowledge => {
                  log.info("Restarted unit: " + unitName)
                  context.system.shutdown()
                }
                case Reject(reason) =>
                  log.info(s"Couldn't restart the $unitName because of: " + reason.getOrElse("Unknown"))
                  context.system.shutdown()
              }
        }
        case _ => {
          throw new Exception("Unknown command!")
          context.system.shutdown()
        }
      }

    }
    case _ => log.error("Interactive actor got message. This shouldn't happen...")
  }

}