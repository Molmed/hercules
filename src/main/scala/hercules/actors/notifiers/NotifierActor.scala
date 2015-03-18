package hercules.actors.notifiers

import akka.actor.{ ActorSystem, ActorRef, Actor, ActorLogging }
import akka.event.LoggingReceive
import hercules.config.notification.NotificationConfig
import hercules.protocols.HerculesMainProtocol.{ SendNotificationUnitMessage, FailedNotificationUnitMessage }
import scala.concurrent.duration._

/**
 * Send notifications of events (e.g. email them or push cards around on a
 * trello board). All specific implementations of notifier Actors should extend
 * the NotifierActor trait. Note that it does not extend the HerculesActor since
 * that would cause circular dependencies.
 */
trait NotifierActor extends Actor with ActorLogging {

  /**
   * The executor which will be used to to the actual lifting of the notifier actor.
   * @return a reference to a executor actor
   */
  def executor: ActorRef

  /**
   * The configuration for this particular notifier.
   * @return a configuration for the notifier
   */
  def notifierConfig: NotificationConfig

  /**
   * Check if this particular notifier is subscribing to the channel
   * on which the message is being sent, and if so forward it to the
   * executor.
   *
   * Will also handle sending retries up until the specified maximum
   * number of tries.
   *
   * Handles messages of types:
   * SendNotificationUnitMessage, FailedNotificationUnitMessage
   */
  override def receive: Receive = LoggingReceive {

    case failedNotificationUnit: FailedNotificationUnitMessage => {
      import context.dispatcher

      if (failedNotificationUnit.unit.attempts <= notifierConfig.numRetries)
        context.system.scheduler.scheduleOnce(
          notifierConfig.retryInterval.seconds,
          self,
          SendNotificationUnitMessage(failedNotificationUnit.unit))
      else
        log.warning(
          s"Reached maximum number of send retries on ${failedNotificationUnit.unit} " +
            "will drop it."
        )
    }

    case notificationMessage: SendNotificationUnitMessage =>
      if (notifierConfig.channels.contains(notificationMessage.unit.channel)) {
        log.debug("Found message on subscription channel will forward it " +
          "to the executor.")
        // Pass the message to the executor
        executor ! notificationMessage
      }
  }
}
