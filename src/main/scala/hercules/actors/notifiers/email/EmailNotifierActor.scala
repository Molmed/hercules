package hercules.actors.notifiers.email

import akka.actor.{ ActorRef, ActorSystem, Props }
import com.typesafe.config.{ Config, ConfigFactory }
import hercules.actors.notifiers.NotifierActor
import hercules.config.notification.{ EmailNotificationConfig, _ }

/**
 * Provides factor methods to create a EmailNotifierActor
 */
object EmailNotifierActor {

  /**
   *
   * @param system the actor system to email notifier in
   * @param config a configuration for the emailer
   * @return A reference to a email notifier actor
   */
  def apply(implicit system: ActorSystem, config: EmailNotificationConfig = EmailNotificationConfig.getEmailNotificationConfig()): ActorRef = {

    lazy val emailNotifierActor = new NotifierActor {
      override def executor: ActorRef = system.actorOf(EmailNotifierExecutorActorImpl.props(config))
      override def notifierConfig: NotificationConfig = config
    }

    system.actorOf(Props(emailNotifierActor))
  }
}
