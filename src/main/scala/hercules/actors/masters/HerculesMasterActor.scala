package hercules.actors.masters

import akka.actor.ActorContext
import hercules.actors.HerculesActor

/**
 * Hercules master actors should define a workflow to be run, e.g.
 * the Sisyphus workflow (as defined in the SisypshusMasterActor).
 */
abstract class HerculesMasterActor extends HerculesActor {}