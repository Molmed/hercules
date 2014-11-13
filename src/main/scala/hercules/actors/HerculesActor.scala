package hercules.actors

import akka.actor.Actor
import akka.actor.ActorLogging
import hercules.actors.notifiers.NotifierManager

/**
 *  The base trait for all Hercules actors. All actors (which are not
 *  spinned up in a very local context, e.g. annonymous actors) should extend
 *  this class. Another exception to this is the notification actors should not
 *  extend this trait, as they are attached to it via the notice field.
 */
trait HerculesActor extends Actor with ActorLogging {

  /**
   * This field is the connection to the notification system.
   * Use the different notification levels, e.g. notice.info("I'm alive")
   *  to forward messages to the notification system, which will then decide
   *  based on the message level what to do with it (i.e. send emails, etc).
   */
  val notice = NotifierManager(context.system)
}