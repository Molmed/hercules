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
import akka.pattern.ask
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

    val primarySeedNode = conf.getStringList("master.akka.cluster.seed-nodes").head
    
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
  def findMessagesOfType[A <: HerculesMessage](messageSeq: Set[HerculesMessage]): Set[A] = {
    messageSeq.filter(p => p.isInstanceOf[A]).map(p => p.asInstanceOf[A])
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

  // The master will register it self to the cluster receptionist.
  ClusterReceptionistExtension(context.system).registerService(self)

  var messagesNotYetProcessed: Set[HerculesMessage] = Set()

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
            messagesNotYetProcessed = messagesNotYetProcessed - unitMessage
            log.info(s"State of queue was: $messagesNotYetProcessed")
          }
          case Reject =>
            log.info(s"$unitMessage was not accepted by demultiplexer. Keep it in the work queue.")
        }
      }
    }

    case FinishedDemultiplexingProcessingUnitMessage(unit) => {
      //@TODO Later more behaviour downstream of demultiplexing should
      // be added here!
      log.info("Noted that " + new File(unit.uri).getName() + " has finished " +
        " demultiplexing. Right now I'll do nothing about.")
    }

  }

}