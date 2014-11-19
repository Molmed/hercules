package hercules

/**
 * Roles indicating which Hercules roles to run.
 */
object HerculesStartRoles {

  /**
   * The base trait used for all roles to run.
   */
  sealed trait Role

  /**
   * Run the master role
   */
  case class RunMaster extends Role

  /**
   * Run the demultiplexer role
   */
  case class RunDemultiplexer extends Role

  /**
   * Run the watcher role
   */
  case class RunRunfolderWatcher extends Role

  /**
   * Run the REST API role
   */
  case class RestApi extends Role

  /**
   * Output the help message
   */
  case class RunHelp extends Role

}