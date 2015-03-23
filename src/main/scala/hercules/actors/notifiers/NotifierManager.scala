package hercules.actors.notifiers

import akka.actor.ActorRef
import akka.actor.ActorSystem
import hercules.actors.notifiers.email.EmailNotifierActor
import hercules.actors.notifiers.rest.slack.SlackNotifierActor
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
   * @param msg string to pass on
   * @param originalMessage optionally add the original message
   * @param channel on which to send message
   * @param actors to send it to
   */
  def sendMessage(
    msg: String,
    originalMessage: Option[HerculesMessage] = None,
    channel: NotificationChannel,
    actors: Seq[ActorRef]): Unit = {
    val message = new SendNotificationUnitMessage(
      new NotificationUnit(
        msg,
        channel,
        originalMessage = originalMessage
      ))
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

  // TODO Make this configurable and start via reflections /JD 20150318
  val actors = Seq(EmailNotifierActor(system), SlackNotifierActor(system))

  /**
   * Send messages on the info channel
   * Use this for things than might be interesting to know (but which are not
   * extremely critical).
   * @param msg
   */
  def info(msg: String): Unit = {
    NotifierManager.sendMessage(msg = msg, channel = Info, actors = actors)
  }

  /**
   * Send string messages on the progress channel
   * This can be used when there is not so much need for downstream
   * actors to know the exact type of message sent.
   * If you need a higher specificity (e.g. to condition behaviour)
   * use the progress(msg: String, originalMessage: HerculesMessage) function.
   * @param msg a string message to send.
   */
  def progress(msg: String): Unit = {
    NotifierManager.sendMessage(msg = msg, channel = Progress, actors = actors)
  }

  /**
   * Send messages on the progress channel
   * Use this to indicate progress of some kind, e.g. that a runfolder has
   * finished processing.
   * @param msg A simple string message
   * @param originalMessage the specific message to pass on (can be used to trigger
   *                        specific behaviour downstream.
   */
  def progress(msg: String, originalMessage: HerculesMessage): Unit = {
    NotifierManager.sendMessage(
      msg = msg, originalMessage = Some(originalMessage), channel = Progress, actors = actors)
  }

  /**
   * Send messages on the warning channel
   * Send non-critical messages about problems on this channel.
   * @param msg
   */
  def warning(msg: String): Unit = {
    NotifierManager.sendMessage(msg = msg, channel = Warning, actors = actors)
  }

  /**
   * Send messages on the critical channel
   * Use this for really important things (such as errors from which we cannot
   * recover).
   * @param msg
   */
  def critical(msg: String): Unit = {
    NotifierManager.sendMessage(msg = msg, channel = Critical, actors = actors)
  }

}
