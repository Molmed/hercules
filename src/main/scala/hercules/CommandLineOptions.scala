package hercules

import hercules.HerculesStartRoles.Role

/**
 * Container class for command line options
 * @param applicationType
 */
case class CommandLineOptions(
  applicationType: Option[List[Role]] = None)