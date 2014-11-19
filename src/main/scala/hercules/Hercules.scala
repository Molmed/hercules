package hercules

/**
 * The main entry point for the application
 *
 * Will parse the command line options and initiate the appropriate
 * system depending on the command and options passed.
 */
object Hercules extends App with HerculesEntryPoint {

  parser.parse(args, CommandLineOptions()) map { config =>
    // do stuff
    parseCommandLineOptions(config)
  }

}