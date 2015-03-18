package hercules.actors.notifiers

import akka.actor.ActorRef
import akka.actor.ActorSystem
import hercules.actors.notifiers.email.EmailNotifierActor
import hercules.entities.notification._
import hercules.protocols.HerculesMainProtocol._
import hercules.protocols.NotificationChannelProtocol._

/**
 * Provides factory and utility methods for the NotifierManager.
 */
object NotifierManager {

  /**
   * Create a NotifierManager
   *
   * @param system
   * @return A NotifierManager
   */
  def apply(system: ActorSystem): NotifierManager = {
    new NotifierManager(system)
  }

  /**
   * Send a message on to the selected channel
   *
   * @param msg
   * @param channel
   * @param actors
   */
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

/**
 * A notifier manager will start up a number of notifier subsystems, which
 * can be of different types (for example sending emails, or moving cards
 * on a trello board). It will provide functions for sending messaged onto
 * these sub-systems on different channels, which will decided if they will
 * forward them or not depending on which channels they are registered to.
 *
 * @param system The actor system to start up the notificaion subsystems in.
 */
class NotifierManager(system: ActorSystem) {

  // TODO Make this configurable / JD 20150317
  val actors = Seq(EmailNotifierActor(system))

  /**
   * Send messages on the info channel
   * Use this for things than might be interesting to know (but which are not
   * extremely critical).
   * @param msg
   */
  def info(msg: String): Unit = {
    NotifierManager.sendMessage(msg, Info, actors)
  }

  /**
   * Send messages on the progress channel
   * Use this to indicate progress of some kind, e.g. that a runfolder has
   * finished processing.
   * @param msg
   */
  def progress(msg: String): Unit = {
    NotifierManager.sendMessage(msg, Progress, actors)
  }

  /**
   * Send messages on the warning channel
   * Send non-cricital messages about problems on this channel.
   * @param msg
   */
  def warning(msg: String): Unit = {
    NotifierManager.sendMessage(msg, Warning, actors)
  }

  /**
   * Send messages on the critical channel
   * Use this for really important things (such as errors from which we cannot
   * recover).
   * @param msg
   */
  def critical(msg: String): Unit = {
    NotifierManager.sendMessage(msg, Critical, actors)
  }

}
