package hercules.actors.notifiers

import akka.actor.ActorRef
import akka.actor.ActorSystem
import hercules.entities.notification._
import hercules.protocols.HerculesMainProtocol._
import hercules.protocols.NotificationChannelProtocol._

object NotifierManager {
  def getInstance(system: ActorSystem): NotifierManager = {
    new NotifierManager(system)
  }

  def sendMessage(
    msg: String,
    channel: NotificationChannel,
    actors: Seq[ActorRef]): Unit = {
    val message = new SendNotificationUnitMessage(
      new NotificationUnit(
        msg,
        channel))
    actors.foreach { _ ! message }
  }
}

class NotifierManager(system: ActorSystem) {

  val actors = Seq(EmailNotifierActor.startInstance(system))

  def info(msg: String): Unit = {
    NotifierManager.sendMessage(msg, Info, actors)
  }
  def progress(msg: String): Unit = {
    NotifierManager.sendMessage(msg, Progress, actors)
  }
  def warning(msg: String): Unit = {
    NotifierManager.sendMessage(msg, Warning, actors)
  }
  def critical(msg: String): Unit = {
    NotifierManager.sendMessage(msg, Critical, actors)
  }

}
