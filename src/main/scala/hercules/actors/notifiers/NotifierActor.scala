package hercules.actors.notifiers

import akka.actor.Actor
import akka.actor.ActorLogging

/**
 * Send notifications of events (e.g. email them or push cards around on a
 * trello board). All specific implementations of notifier Actors should extend
 * the NotifierActor trait. Note that it does not extend the HerculesActor since
 * that would cause circular dependencies (?)
 */
trait NotifierActor extends Actor with ActorLogging {}
