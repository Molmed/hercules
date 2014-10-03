package hercules.api

import hercules.actors.utils.MasterLookup

/**
 * This trait contains the actors that make up our application; it can be mixed in with
 * ``BootedCore`` for running code or ``TestKit`` for unit and integration tests.
 */
trait CoreActors extends MasterLookup {
  this: Core =>

  val cluster = getMasterClusterClient(system, getDefaultConfig, getDefaultClusterClient)
  
}