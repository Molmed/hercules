package hercules.actors.notifiers

import hercules.actors.HerculesActor
import akka.actor.ActorRef
import akka.actor.Actor
import hercules.entities.notification.NotificationUnit
import hercules.protocols.HerculesMainProtocol._
import hercules.protocols.NotificationChannelProtocol._
  
/**
 * Send notifications of events (e.g. email them or push cards around on a
 * trello board).
 */
trait Notifier {

  // Provide a notify handle 
  protected val notice = NotifierManager.getInstance
} 


object NotifierManager {
  def getInstance: NotifierManager = {
    new NotifierManager
  }

  def send_message(
    msg: String,
    channel: NotificationChannel,  
    actors: Seq[ActorRef]): Unit = {
      val message = new SendNotificationUnitMessage(
        new NotificationUnit(
          msg,
          channel))
      actors.foreach {_ ! message}
    }  
}

class NotifierManager {
  val actors = Seq(EmailNotifierActor.startInstance("EmailNotifierActor"))
  
  def info(msg: String): Unit = {
    NotifierManager.send_message(msg,Info,actors)
  }
  def progress(msg: String): Unit = {
    NotifierManager.send_message(msg,Progress,actors)
  }
  def warning(msg: String): Unit = {
    NotifierManager.send_message(msg,Warning,actors)
  }
  def critical(msg: String): Unit = {
    NotifierManager.send_message(msg,Critical,actors)
  }

}
