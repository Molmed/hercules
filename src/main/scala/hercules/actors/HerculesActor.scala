package hercules.actors

import akka.actor.{Actor, ActorLogging}
import hercules.actors.notifiers.NotifierManager
import hercules.utils.VersionUtils

/**
 *  The base trait for all Hercules actors. All actors (which are not
 *  spun up in a very local context, e.g. anonymous actors) should extend
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

  /**
   * This will make sure that the log gets an entry with the
   * version of hercules run, every time a HerculesActor is started.
   */
  log.info("Hercules version: " + VersionUtils.herculesVersion)
}