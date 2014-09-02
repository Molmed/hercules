package hercules.actors.notifiers

import hercules.actors.HerculesActor

/**
 * Send notifications of events (e.g. email them or push cards around on a
 * trello board).
 */
trait NotifierActor extends HerculesActor {

  def receive = ???
  
}