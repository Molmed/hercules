package hercules.actors.notifiers

import akka.actor.Props
import hercules.actors.HerculesActor
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit
import hercules.protocols.HerculesMainProtocol

object EmailNotifierExecutorActor {
  
  def props(emailConfig: EmailNotificationConfig): Props = {
    Props(new EmailNotifierExecutorActor(emailConfig))
  }
  
}

class EmailNotifierExecutorActor(
  emailConfig: EmailNotificationConfig) extends HerculesActor {
  
  import HerculesMainProtocol._
    
  def receive = {
    case message: SendNotificationUnitMessage => {
      log.info(self.getClass().getName() + " received a " + message.getClass().getName() + " with a " + message.unit.getClass().getName())
      message.unit match {
        case unit: EmailNotificationUnit => {
          // If we manage to send the message, send a confirmation
          if (EmailNotificationUnit.sendNotification(unit)) {
            sender ! SentNotificationUnitMessage(unit)
          }
          // Else, increase the attempts counter and send a failure message
          else {
            unit.attempts = unit.attempts + 1
            sender ! FailedNotificationUnitMessage(unit,"Just failed")
          }
        }
        case message => {
          log.info(self.getClass().getName() + " received a " + message.getClass().getName() + " message: " + message.toString() + " and ignores it")
        }
      } 
    }
    case message => {
      log.info(self.getClass().getName() + " received a " + message.getClass().getName() + " message: " + message.toString() + " and ignores it")
    }
  }
} 
