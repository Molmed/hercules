package hercules.actors.notifiers.rest.slack

import akka.actor.{ Props, ActorRef, ActorSystem }
import hercules.actors.notifiers.NotifierActor
import hercules.config.notification.{ SlackNotificationConfig, NotificationConfig }

/**
 * Created by johda411 on 2015-03-18.
 * Factory to create the concrete implementation of the SlackNotifierActor.
 */
object SlackNotifierActor {

  /**
   * @param system to create actor in.
   * @param config to use (will get it's info from application.conf be default)
   * @return a reference to an instance of SlackNotificationActor.
   */
  def apply(implicit system: ActorSystem, config: SlackNotificationConfig = SlackNotificationConfig()): ActorRef = {

    def slackNotifierActor = new NotifierActor {
      override def executor: ActorRef = system.actorOf(SlackNotifierExecutor.props(config))
      override def notifierConfig: NotificationConfig = config
    }

    system.actorOf(Props(slackNotifierActor))
  }

}
