package hercules.actors.masters

import akka.actor.ActorContext
import hercules.actors.HerculesActor
import hercules.actors.masters.state.MasterState

/**
 * Hercules master actors should define a workflow to be run, e.g.
 * the Sisyphus workflow (as defined in the SisypshusMasterActor).
 */
trait HerculesMasterActor extends HerculesActor {

  // The state of the actor, containing messages which have not yet been
  // processed, as well as failed messages.
  var state = MasterState()

}