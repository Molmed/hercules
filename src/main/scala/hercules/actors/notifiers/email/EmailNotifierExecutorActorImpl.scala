package hercules.actors.notifiers.email

import akka.actor.Props
import akka.event.LoggingReceive
import hercules.actors.notifiers.NotificationExecutor
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit

/**
 * Provided factory methods for creating EmailNotifierExecutorActors
 */
object EmailNotifierExecutorActorImpl {

  /**
   * Get a props for creating a EmailNotifierExecutorActor.
   * @param emailConfig
   * @return Props for creating a EmailNotifierExecutorActor
   */
  def props(emailConfig: EmailNotificationConfig): Props = {
    Props(new EmailNotifierExecutorActorImpl(emailConfig))
  }

}

/**
 * A actor which can send emails based on a SendNotificationUnitMessage.
 *
 * @param emailConfig
 */
class EmailNotifierExecutorActorImpl(val emailConfig: EmailNotificationConfig) extends EmailNotifierExecutorActor with NotificationExecutor with Emailer
