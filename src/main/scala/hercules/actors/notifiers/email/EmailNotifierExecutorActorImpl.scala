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
   * Get a props for creating a EmailNotifierExecutorActorImpl.
   * @param emailConfig the email configuration.
   * @return Props for creating a EmailNotifierExecutorActor
   */
  def props(emailConfig: EmailNotificationConfig): Props = {
    Props(new EmailNotifierExecutorActorImpl(emailConfig))
  }

}

/**
 * A actor which can send emails based on a SendNotificationUnitMessage.
 * This is probably the class you want if you are looking for a default
 * implementation of the EmailNotifierActor trait.
 *
 * @param emailConfig The email configuration.
 */
class EmailNotifierExecutorActorImpl(val emailConfig: EmailNotificationConfig) extends EmailNotifierExecutorActor with NotificationExecutor with Emailer
