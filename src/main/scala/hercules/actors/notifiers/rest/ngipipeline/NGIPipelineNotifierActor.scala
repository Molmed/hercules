package hercules.actors.notifiers.rest.ngipipeline

import akka.actor.{ Props, ActorRef, ActorSystem }
import hercules.actors.notifiers.NotifierActor
import hercules.config.notification.{ NGIPipelineNotificationConfig, NotificationConfig }
import hercules.protocols.HerculesMainProtocol.{ FinishedDemultiplexingProcessingUnitMessage, SendNotificationUnitMessage }

/**
 * A notifier actor to communicate with the ngi pipline. Specifically made to
 * automatically trigger analysis for WGS data.
 *
 * Created by johda411 on 2015-03-20.
 */
object NGIPipelineNotifierActor {

  /**
   * We only want to send on messages to the executor which are related to
   * starting finished demultiplexing a runfolder. (Since that means that we can
   * proceed to start the NGI pipeline).
   * @param notificationMessage to check for inclusion
   * @return true if the messages is to be kept.
   */
  def notifierActorChannelPreFilter(notificationMessage: SendNotificationUnitMessage): Boolean = {
    val originalMsg = notificationMessage.unit.originalMessage
    originalMsg.isDefined && originalMsg.get.isInstanceOf[FinishedDemultiplexingProcessingUnitMessage]
  }

  /**
   * @param system to create actor in.
   * @param config to use (will get it's info from application.conf be default)
   * @return a reference to an instance of NGIPipelineNotification.
   */
  def apply(implicit system: ActorSystem, config: NGIPipelineNotificationConfig = NGIPipelineNotificationConfig()): ActorRef = {

    def ngiNotifierActor = new NotifierActor {
      override def executor: ActorRef = system.actorOf(NGIPipelineNotifierExecutor.props(config))
      override def notifierConfig: NotificationConfig = config
      override def preFilterChannel(notificationMessage: SendNotificationUnitMessage): Boolean = {
        notifierActorChannelPreFilter(notificationMessage)
      }
    }

    system.actorOf(Props(ngiNotifierActor))
  }

}
