package hercules.actors.notifiers

import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.event.Logging
import akka.contrib.pattern.ClusterClient.SendToAll
import scala.concurrent.duration._
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification._
import hercules.protocols._
import hercules.config.notification._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorLogging

object EmailNotifierActor {

  /**
   * Initiate all the stuff needed to start a EmailNotifierActor
   * including initiating the system.
   */

  def startInstance(
    system: ActorSystem = ActorSystem("EmailNotifierSystem")
    ): ActorRef = {
      system.actorOf(
        props(), 
        "EmailNotifierActor"
      )
  }

  /**
   * Create a new EmailNotifierActor
   * @param clusterClient A reference to a cluster client thorough which the
   *                      actor will communicate with the rest of the cluster.
   */
  def props(): Props = {
    Props(new EmailNotifierActor())
  }  
}

class EmailNotifierActor() extends NotifierActor {

  var failedNotifications: Set[EmailNotificationUnit] = Set()
  var sentNotifications: Set[EmailNotificationUnit] = Set()
  var permanentlyFailedNotifications: Set[EmailNotificationUnit] = Set()
  
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
  

  // Periodically attempt to resend failed messages up to a limit
  val resendFailed =
    context.system.scheduler.schedule(
      Duration.create(emailConfig.retryInterval,"seconds"),
      Duration.create(emailConfig.retryInterval,"seconds"), 
      self, 
      {RetryFailedNotificationUnitsMessage()})

  // Make sure that the scheduled event stops if the actors does.
  override def postStop() = {
    resendFailed.cancel()
  }
  
  
  def receive = {

    // If we receive an instruction to retry failed messages, iterate over that set and send messages that have not met the limit for maximum number of retries
    case _: RetryFailedNotificationUnitsMessage => {
      log.debug(self + " received a RetryFailedNotificationUnitsMessage")
      // Send a notification message and remove the unit from the list
      failedNotifications.foreach(
        unit => {
          self ! SendNotificationUnitMessage(unit)
          failedNotifications = failedNotifications.filterNot(u => u == unit)
        })
    }
    
    // We've received a request to send a notification
    case message: SendNotificationUnitMessage => message.unit match {
      // Check if the message unit is already an EmailNotificationUnit or if we need to wrap it
      case unit: EmailNotificationUnit => {
        // Check if the notification unit is in a channel that we will pay attention to
        if (emailConfig.channels.contains(message.unit.channel)) {
          log.debug(self.getClass.getSimpleName + " will attempt for the " + unit.attempts + " time to deliver " + unit.getClass.getSimpleName + " message: " + unit.message)
          // Pass the message to the executor
          notifierRouter ! message
        }
        else {
          log.debug(self.getClass.getSimpleName + " does not listen to the " + message.unit.channel + " channel and will ignore message")
        }
      }
      // Wrap the notification unit to an email
      case unit: NotificationUnit => {
        // Wrap the message and send it to self
        self ! new SendNotificationUnitMessage(EmailNotificationUnit.wrapNotificationUnit(message.unit))
      }
    }

    // If we receive a failure message, log the failure and add it to the failed set
    case message: FailedNotificationUnitMessage => message.unit match {
      case unit: EmailNotificationUnit => {
        log.debug(self.getClass.getSimpleName + " received a " + message.getClass.getSimpleName + " reason: " + message.reason)
        (unit.attempts - 1) match {
          case emailConfig.numRetries => 
            permanentlyFailedNotifications = permanentlyFailedNotifications + unit
          case _ =>
            failedNotifications = failedNotifications + unit
        }
      }
    }

    // If we receive a send confirmation message, add the message to the sent set
    case message: SentNotificationUnitMessage => message.unit match {
      case unit: EmailNotificationUnit => {
        log.debug(self.getClass.getSimpleName + " received a " + unit.getClass.getSimpleName)
        sentNotifications = sentNotifications + unit
      }
    }
    
    case message => {
      log.debug(self.getClass.getSimpleName + " received a " + message.getClass.getSimpleName + " which will be ignored")
    }
  }
}
