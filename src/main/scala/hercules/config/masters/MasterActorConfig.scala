package hercules.config.masters

/**
 * Configure the master actor
 *
 * @param snapshotInterval how often to snapshot the state.
 */
case class MasterActorConfig(snapshotInterval: Int) {

}