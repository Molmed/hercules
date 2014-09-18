package hercules.actors.masters

import java.io.File
import scala.collection.JavaConversions.asScalaBuffer
import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.contrib.pattern.ClusterSingletonManager
import hercules.protocols.HerculesMainProtocol._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.collection.JavaConversions._

object SisyphusMasterActor {

  /**
   * Initiate all the stuff needed to start a SisyphusMasterActor
   * including initiating the system.
   */
  def startSisyphusMasterActor(): Unit = {

    val generalConfig = ConfigFactory.load()
    val conf = generalConfig.getConfig("master").withFallback(generalConfig)

    val system = ActorSystem("ClusterSystem", conf)

    system.actorOf(
      ClusterSingletonManager.props(
        SisyphusMasterActor.props(),
        "active",
        PoisonPill,
        Some("master")),
      "master")
  }

  /**
   * Create a new SisyphusMasterActor
   */
  def props(): Props = Props(new SisyphusMasterActor())

  /**
   * Filter out the messages with conform to type A from a list of
   * messages.
   * @param messageSeq
   * @return All messages of type A
   */
  def findMessagesOfType[A <: ProcessingUnitMessage](messageSeq: Set[ProcessingUnitMessage]): Set[A] = {
    messageSeq.filter(p => p.isInstanceOf[A]).map(p => p.asInstanceOf[A])
  }

  /**
   * Internal messaging protocol
   */
  object SisyphusMasterActorProtocol {
    sealed trait SetMessageState
    case class RemoveFromMessageNotYetProcessed(message: ProcessingUnitMessage) extends SetMessageState
    case class RemoveFromFailedMessages(message: ProcessingUnitMessage) extends SetMessageState
  }

}

/**
 * Defines the logic for running the Sisyphus workflow
 *
 * The other actors in the system will register to the master,
 * and request work from it. If the master has work for the actor it will
 * send it.
 */
class SisyphusMasterActor extends HerculesMasterActor {

  import SisyphusMasterActor.SisyphusMasterActorProtocol._

  // The master will register it self to the cluster receptionist.
  ClusterReceptionistExtension(context.system).registerService(self)

  var messagesNotYetProcessed: Set[ProcessingUnitMessage] = Set()
  var failedMessages: Set[ProcessingUnitMessage] = Set()

  def receive = {

    case StringMessage(s) =>
      log.info(s"I got this message: $s")

    case message: FoundProcessingUnitMessage => {
      log.info("Sisyphus master got a FoundProcessingUnitMessage: " + message)
      messagesNotYetProcessed = messagesNotYetProcessed + message
    }

    case RequestDemultiplexingProcessingUnitMessage => {
      log.info("Got a request for a ProccesingUnit to demultiplex.")

      val unitsReadyForDemultiplexing = SisyphusMasterActor.
        findMessagesOfType[FoundProcessingUnitMessage](messagesNotYetProcessed)

      import context.dispatcher
      implicit val timeout = Timeout(5 seconds)

      for (unitMessage <- unitsReadyForDemultiplexing) {
        (sender ? StartDemultiplexingProcessingUnitMessage(unitMessage.unit)).map {
          case Acknowledge => {
            log.info(s"$unitMessage was accepted by demultiplexer removing from work queue.")
            RemoveFromMessageNotYetProcessed(unitMessage)            
          }
          case Reject =>
            log.info(s"$unitMessage was not accepted by demultiplexer. Keep it in the work queue.")
        } pipeTo(self)
      }
    }

    case x: SetMessageState => {
      x match {
        case RemoveFromMessageNotYetProcessed(message) => {
          messagesNotYetProcessed = messagesNotYetProcessed - message
        }          
        case RemoveFromFailedMessages(message) => 
          failedMessages = failedMessages - message
      }
    }

    case FinishedDemultiplexingProcessingUnitMessage(unit) => {
      //@TODO Later more behaviour downstream of demultiplexing should
      // be added here!
      log.info("Noted that " + unit.name + " has finished " +
        " demultiplexing. Right now I'll do nothing about.")
    }

    case message: FailedDemultiplexingProcessingUnitMessage => {
      //@TODO This would be a perfect place to run send a notification :D
      log.warning("Noted that " + message.unit.name + " has failed " +
        " demultiplexing. Will move it into the list of failed jobs.")
      failedMessages = failedMessages + message
    }

    case message: RestartDemultiplexingProcessingUnitMessage => {
      if (failedMessages.exists(p => p.unit.name == message.unitName)) {
        log.info(
          "For a message to restart " + message.unitName +
            " moving it into the messages to process list.")

        val matchingMessage = failedMessages.find(x => x.unit.name == message.unitName).get
        val startDemultiplexingMessage = new StartDemultiplexingProcessingUnitMessage(matchingMessage.unit)
        messagesNotYetProcessed = messagesNotYetProcessed + startDemultiplexingMessage
        failedMessages = failedMessages - matchingMessage
        sender ! Acknowledge
      } else {
        log.info("Couldn't find unit " + message.unitName + " requested to restart.")
        sender ! Reject(Some("Couldn't find unit " + message.unitName + " requested to restart."))
      }

    }

    case message => log.info(self.getClass().getName() + " received a " + message.getClass().getName() + " message which will be ignored")
  }
}