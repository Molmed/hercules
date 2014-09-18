package hercules.actors.notifiers

import akka.actor.Props
import akka.actor.ActorRef
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.routing.RoundRobinRouter
import scala.concurrent.duration._
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit
import hercules.actors.utils.MasterLookup
import hercules.protocols.HerculesMainProtocol
import akka.actor.ActorSystem

object EmailNotifierActor extends MasterLookup {

  /**
   * Initiate all the stuff needed to start a EmailNotifierActor
   * including initiating the system.
   */

  def startEmailNotifierActor(): ActorRef = {

    val system = ActorSystem("EmailNotifierSystem")
    
    val clusterClient = getMasterClusterClient(system)

    val selfref = system.actorOf(
      props(clusterClient), 
      "EmailNotifierActor"
    )
    
    selfref ! "EmailNotifierActor started"
    selfref
  }

  /**
   * Create a new EmailNotifierActor
   * @param clusterClient A reference to a cluster client thorough which the
   *                      actor will communicate with the rest of the cluster.
   */
  def props(clusterClient: ActorRef): Props = {
    Props(new EmailNotifierActor(clusterClient))
  }
  
}

class EmailNotifierActor(
  clusterClient: ActorRef) extends NotifierActor {

  import HerculesMainProtocol._

  // Get a EmailNotifierConfig object
  val emailConfig = EmailNotificationConfig.getEmailNotificationConfig(
    context.system.settings.config.getConfig("email")
  )
  // Spawn an executor object that will do the work for us
  val notifierRouter = context.actorOf(
    EmailNotifierExecutorActor.props(
      emailConfig).withRouter(
        RoundRobinRouter(nrOfInstances = 1)
    ),
    "EmailNotifierExecutorActor"
  )

  import context.dispatcher

  // Request new work periodically
  val requestWork =
    context.system.scheduler.schedule(
      10.seconds, 
      10.seconds, 
      self,
      RequestNotificationUnitMessage()
    )

	// Create some artificial tasks
	val createWork =
    	context.system.scheduler.schedule(
      	60.seconds, 
      	300.seconds, 
      	self,
      	SendNotificationUnitMessage(new EmailNotificationUnit("Generated work"))
    )

	

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    requestWork.cancel()
  }
  
  def receive = {
    
    // We've received a request to send a notification
    case message: SendNotificationUnitMessage => {
      // Check if the notification unit is an email
      message.unit match {
        case unit: EmailNotificationUnit => {
          log.info(self.getClass().getName() + " will attempt for the " + unit.attempts + " time to deliver " + unit.getClass().getName() + " message: " + unit.message)
          // Acknowledge to sender that we will take this
          sender ! Acknowledge
          // Pass the message to the executor
          notifierRouter ! message
        }
        // If message unit is not an EmailNotificationUnit, we'll reject it 
        case unit => {  
          log.info(self.getClass().getName() + " will not process NotificationUnit of type " + unit.getClass().getName() + ", rejecting")
          sender ! Reject
        }
      }
    }
    // If we receive a failure message, pass it on to the master
    case message: FailedNotificationUnitMessage => {
      log.info(self.getClass().getName() + " passes failed  " + message.unit.getClass().getName() + " on up to master")
      clusterClient ! SendToAll("/user/master/active",message)
    }
    // If we receive a send confirmation message, pass it on to master
    case message: SentNotificationUnitMessage => {
      log.info(self.getClass().getName() + " passes sent  " + message.unit.getClass().getName() + " on up to master")
      clusterClient ! SendToAll("/user/master/active",message)
    }
    // If we receive a request for messages to send, pass it on to master
    case message: RequestNotificationUnitMessage => {
      log.info(self.getClass().getName() + " passes " + message.getClass().getName() + " on up to master")
      clusterClient ! SendToAll("/user/master/active",message)
    }
    case message => {
      log.info(self.getClass().getName() + " received a " + message.getClass().getName() + " message: " + message.toString() + " and ignores it")
    }
  }
}
