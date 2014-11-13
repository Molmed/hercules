package hercules.config.masters

/**
 * TODO I'm not sure that this is necessary. Maybe better to just use to
 * the Typesafe Config directly instead? /JD 20141113
 * 
 * Configure the master actor
 *
 * @param snapshotInterval how often to snapshot the state.
 */
case class MasterActorConfig(snapshotInterval: Int) {

}