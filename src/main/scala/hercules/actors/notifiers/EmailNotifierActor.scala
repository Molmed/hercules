package hercules.actors.notifiers

import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.event.LoggingReceive
import scala.concurrent.duration._
import scala.util.Random
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification._
import hercules.protocols._
import hercules.config.notification._
import com.typesafe.config.{ Config, ConfigFactory }

/**
 * Provides factor methods to create a EmailNotifierActor
 */
object EmailNotifierActor {

  /**
   * Initiate all the stuff needed to start a EmailNotifierActor
   * including initiating the system.
   * @param system The actor system that the notifier should start in.
   * @param config A config instance (will default to loading the default
   * 			   application config).
   * @return A reference to a EmailNotifierActor
   */
  def startInstance(
    system: ActorSystem,
    config: Config = ConfigFactory.load()): ActorRef = {
    val conf = EmailNotificationConfig.getEmailNotificationConfig(
      config.getConfig("notifications.email"))
    val executor: ActorRef = system.actorOf(
      EmailNotifierExecutorActor.props(conf))
    // Append a random string to the actor name to ensure it is unique
    system.actorOf(
      props(conf, executor),
      "EmailNotifierActor_" + List.fill(8)((Random.nextInt(25) + 97).toChar).mkString)
  }

  /**
   * Create a new EmailNotifierActor
   * @param conf A EmailNotificationConfig instance
   * @param executor The email executor actor
   * @return A Props for creating a EmailNotifierActor
   */
  def props(
    conf: EmailNotificationConfig,
    executor: ActorRef): Props = {
    Props(new EmailNotifierActor(conf, executor))
  }
}

/**
 * A EmailNotifierActor will handle the logic surrounding sending emails,
 * but not actually send them itself. The latter will be deferred to the 
 * EmailNotifierExecutorActor.
 * 
 * It will be responsible for handling number of retries of resending emails, etc.
 * It will only send messages which are sent on the channel which it has registered
 * to - as defined in the emailConfig
 * 
 * @param emailConfig
 * @param executor
 */
class EmailNotifierActor(
    val emailConfig: EmailNotificationConfig,
    val executor: ActorRef) extends NotifierActor {

  import context.dispatcher
  import NotificationChannelProtocol._
  import HerculesMainProtocol._

  def receive = LoggingReceive {

    // We've received a request to send a notification
    case message: SendNotificationUnitMessage => message.unit match {
      // Check if the message unit is already an EmailNotificationUnit or if we need to wrap it
      case unit: EmailNotificationUnit => {
        // Check if the notification unit is in a channel that we will pay attention to
        if (emailConfig.channels.contains(message.unit.channel)) {
          // Pass the message to the executor
          executor ! message
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
        (unit.attempts - 1) match {
          case retries if (emailConfig.numRetries > 0 &&
            emailConfig.numRetries == retries) => {
            log.warning("Giving up trying to send " + unit.getClass.getSimpleName)
          }
          case _ =>
            context.system.scheduler.scheduleOnce(
              Duration.create(emailConfig.retryInterval, "seconds"),
              self,
              SendNotificationUnitMessage(unit))
        }
      }
    }
  }
}
