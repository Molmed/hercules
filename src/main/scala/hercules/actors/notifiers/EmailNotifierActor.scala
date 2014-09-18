package hercules.actors.notifiers

import akka.actor.Props
import akka.actor.ActorRef
import akka.event.Logging
import akka.contrib.pattern.ClusterClient.SendToAll
import scala.concurrent.duration._
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification._
import hercules.protocols._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorLogging

object EmailNotifierActor extends ActorFactory {
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

  var failedNotifications: Set[NotificationUnit] = Set()
  var sentNotifications: Set[NotificationUnit] = Set()

  import HerculesMainProtocol._

  // Get a EmailNotifierConfig object
  val emailConfig = EmailNotificationConfig.getEmailNotificationConfig(
    ConfigFactory.load().getConfig("notifications.email")
  )
  // Spawn an executor object that will do the work for us
  val notifierRouter = context.actorOf(
    EmailNotifierExecutorActor.props(
      emailConfig),
    "EmailNotifierExecutorActor"
  )

  import context.dispatcher
  import NotificationChannelProtocol._
  
  def receive = {
    
    // We've received a request to send a notification
    case message: SendNotificationUnitMessage => {
      // Check if the message unit is already an EmailNotificationUnit or if we need to wrap it
      message.unit match {
        case unit: EmailNotificationUnit => {
          log.info(self.getClass().getName() + " will attempt for the " + unit.attempts + " time to deliver " + unit.getClass().getName() + " message: " + unit.message)
          // Pass the message to the executor
          notifierRouter ! message
        }
        // Wrap the notification unit to an email
        case unit: NotificationUnit => {
          // Check if the notification unit is in a channel that we will pay attention to
          if (emailConfig.channels.contains(message.unit.channel)) {
            // Wrap the message and send it to self
            self ! new SendNotificationUnitMessage(EmailNotificationUnit.wrapNotificationUnit(message.unit))
          }
          else {
            log.info(self.getClass().getSimpleName() + " does not listen to the " + message.unit.channel + " channel and will ignore message")
          }
        }
      }
    }
    // If we receive a failure message, log the failure and add it to the failed set
    case message: FailedNotificationUnitMessage => {
      log.info(self.getClass.getSimpleName + " received a " + message.getClass.getSimpleName + " reason: " + message.reason)
      failedNotifications = failedNotifications + message.unit
    }
    // If we receive a send confirmation message, add the message to the sent set
    case message: SentNotificationUnitMessage => {
      log.info(self.getClass.getSimpleName + " received a " + message.unit.getClass.getSimpleName)
      sentNotifications = sentNotifications + message.unit
    }
    // If not a NotificationUnitMessage, we will ignore it
    case message => {
      log.info(self.getClass.getSimpleName + " received a " + message.getClass.getSimpleName + " and ignores it")
    }
  }
}
