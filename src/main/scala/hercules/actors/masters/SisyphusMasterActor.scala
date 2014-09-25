package hercules.actors.masters

import scala.concurrent.duration.DurationInt

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.contrib.pattern.ClusterSingletonManager
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.pattern.pipe
import akka.persistence.PersistentActor
import akka.persistence.SaveSnapshotFailure
import akka.persistence.SaveSnapshotSuccess
import akka.persistence.SnapshotOffer
import akka.util.Timeout
import hercules.config.masters.MasterActorConfig
import hercules.protocols.HerculesMainProtocol._

object SisyphusMasterActor {

  /**
   * Initiate all the stuff needed to start a SisyphusMasterActor
   * including initiating the system.
   */
  def startSisyphusMasterActor(): Unit = {

    val generalConfig = ConfigFactory.load()
    val conf = generalConfig.getConfig("master").withFallback(generalConfig)

    val masterConfig = new MasterActorConfig(conf.getInt("snapshot.interval"))

    val system = ActorSystem("ClusterSystem", conf)

    system.actorOf(
      ClusterSingletonManager.props(
        SisyphusMasterActor.props(masterConfig),
        "active",
        PoisonPill,
        Some("master")),
      "master")
  }

  /**
   * Create a new SisyphusMasterActor
   */
  def props(config: MasterActorConfig): Props = Props(new SisyphusMasterActor(config))

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

    // Use this to save snapshot of the current actor state.
    case object TakeASnapshot

    // These messages are always to be used when updating the state of the 
    // actor. The reason for this is that these messages need to be persisted
    // to be able to able to replay the actors state.
    sealed trait SetStateMessage

    case class AddToMessageNotYetProcessed(message: ProcessingUnitMessage) extends SetStateMessage
    case class RemoveFromMessageNotYetProcessed(message: ProcessingUnitMessage) extends SetStateMessage

    case class AddToFailedMessages(message: ProcessingUnitMessage) extends SetStateMessage
    case class RemoveFromFailedMessages(message: ProcessingUnitMessage) extends SetStateMessage
  }

  /**
   * Captures the current state of the actor.
   */
  case class SisyphusMasterActorState(
      val messagesNotYetProcessed: Set[ProcessingUnitMessage] = Set(),
      val failedMessages: Set[ProcessingUnitMessage] = Set()) {

    import SisyphusMasterActorProtocol._

    /**
     * Manipulate the state of the message sets depending on what message has been
     * sent
     * @param x the SetStateMessage describing the message to add or remove
     * @return Unit
     */
    def manipulateState(x: SetStateMessage): SisyphusMasterActorState = {
      x match {
        case AddToMessageNotYetProcessed(message) =>
          this.copy(messagesNotYetProcessed = messagesNotYetProcessed + message)

        case RemoveFromMessageNotYetProcessed(message) =>
          this.copy(messagesNotYetProcessed = messagesNotYetProcessed - message)

        case AddToFailedMessages(message) =>
          this.copy(failedMessages = failedMessages + message)

        case RemoveFromFailedMessages(message) =>
          this.copy(failedMessages = failedMessages - message)
      }
    }
  }

}

/**
 * Defines the logic for running the Sisyphus workflow
 *
 * The other actors in the system will register to the master,
 * and request work from it. If the master has work for the actor it will
 * send it.
 */
class SisyphusMasterActor(config: MasterActorConfig) extends PersistentActor with HerculesMasterActor {

  override def persistenceId = "SisyphusMasterActor"

  import SisyphusMasterActor.SisyphusMasterActorProtocol._
  import SisyphusMasterActor.SisyphusMasterActorState

  // The master will register it self to the cluster receptionist.
  ClusterReceptionistExtension(context.system).registerService(self)

  import context.dispatcher

  //@TODO Make request new work period configurable.
  // Make sure that the system is snapshoted every now and then
  val saveStateSnapshot =
    context.system.scheduler.schedule(
      config.snapshotInterval.seconds,
      config.snapshotInterval.seconds,
      self,
      TakeASnapshot)

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    saveStateSnapshot.cancel()    
  }

  // The state of the actor, containing messages which have not yet been
  // processed, as well as failed messages.
  var state = SisyphusMasterActorState()

  // Replay messages and snapshots to restore actor state
  override def receiveRecover: Receive = {
    case x: SetStateMessage =>
      state = state.manipulateState(x)
    case SnapshotOffer(_, snapshot: SisyphusMasterActorState) =>
      state = snapshot
  }

  override def receiveCommand: Receive = LoggingReceive {

    // Only messages handled by this method will manipulate the state
    // of the actor, and therefore they need to be persisted
    case message: SetStateMessage => {
      persist(message)(m => state = state.manipulateState(m))
    }

    case TakeASnapshot => {
      saveSnapshot(state)
    }

    case SaveSnapshotFailure(metadata, reason) => {
      log.error(s"Failed to save a snapshot - metadata: $metadata reason: $reason")
    }

    case SaveSnapshotSuccess(_) => {
      log.debug("Yeah! We save that that snapshot!")
    }

    case message: FoundProcessingUnitMessage => {
      self ! AddToMessageNotYetProcessed(message)
    }

    case RequestDemultiplexingProcessingUnitMessage => {

      val unitsReadyForDemultiplexing = SisyphusMasterActor.
        findMessagesOfType[FoundProcessingUnitMessage](state.messagesNotYetProcessed)

      import context.dispatcher
      implicit val timeout = Timeout(5 seconds)

      for (unitMessage <- unitsReadyForDemultiplexing) {
        (sender ? StartDemultiplexingProcessingUnitMessage(unitMessage.unit)).map {
          case Acknowledge => {
            log.debug(s"$unitMessage was accepted by demultiplexer removing from work queue.")
            RemoveFromMessageNotYetProcessed(unitMessage)
          }
          case Reject =>
            log.debug(s"$unitMessage was not accepted by demultiplexer. Keep it in the work queue.")
        } pipeTo (self)
      }
    }

    case FinishedDemultiplexingProcessingUnitMessage(unit) => {
      //@TODO Later more behaviour downstream of demultiplexing should
      // be added here!
      log.debug("Noted that " + unit.name + " has finished " +
        " demultiplexing. Right now I'll do nothing about.")
    }

    case message: FailedDemultiplexingProcessingUnitMessage => {
      //@TODO This would be a perfect place to run send a notification :D
      log.warning("Noted that " + message.unit.name + " has failed " +
        " demultiplexing. Will move it into the list of failed jobs.")        
      self ! AddToFailedMessages(message)
      notice.warning(s"Failed demultiplexing for: $message.unit with the reason: $message.reason")
    }

    // Refer to change state messages.
    case message: RestartDemultiplexingProcessingUnitMessage => {
      if (state.failedMessages.exists(p => p.unit.name == message.unitName)) {
        log.info(
          "For a message to restart " + message.unitName +
            " moving it into the messages to process list.")

        val matchingMessage = state.failedMessages.find(x => x.unit.name == message.unitName).get
        val startDemultiplexingMessage = new StartDemultiplexingProcessingUnitMessage(matchingMessage.unit)
        self ! AddToMessageNotYetProcessed(startDemultiplexingMessage)
        self ! RemoveFromFailedMessages(matchingMessage)
        sender ! Acknowledge
      } else {
        log.warning("Couldn't find unit " + message.unitName + " requested to restart.")
        sender ! Reject(Some("Couldn't find unit " + message.unitName + " requested to restart."))
      }

    }

  }
}