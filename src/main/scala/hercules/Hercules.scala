package hercules

import hercules.actors.demultiplexing.IlluminaDemultiplexingActor
import hercules.actors.masters.SisyphusMasterActor
import hercules.actors.processingunitwatcher.IlluminaProcessingUnitWatcherActor

/**
 * The main entry point for the application
 *
 * Will parse the command line options and initiate the appropriate
 * system depending on the command and options passed.
 */
object Hercules extends App {

  // A very simple command line parser that is able to check if this is a
  // worker or a master that is starting up.
  sealed trait Command
  case object RunMaster extends Command
  case object RunDemultiplexter extends Command
  case object RunRunfolderWatcher extends Command
  case class CommandLineOptions(applicationType: Option[Command] = None)

  val parser = new scopt.OptionParser[CommandLineOptions]("Hercules") {
    cmd("master") action { (_, c) =>
      c.copy(applicationType = Some(RunMaster))
    }
    cmd("demultiplexer") action { (_, c) =>
      c.copy(applicationType = Some(RunDemultiplexter))
    }
    cmd("watcher") action { (_, c) =>
      c.copy(applicationType = Some(RunRunfolderWatcher))
    }
  }

  parser.parse(args, CommandLineOptions()) map { config =>
    // do stuff
    config.applicationType match {
      case Some(RunMaster) =>
        SisyphusMasterActor.startSisyphusMasterActor()
      case Some(RunDemultiplexter) =>
        IlluminaDemultiplexingActor.startIlluminaDemultiplexingActor()
      case Some(RunRunfolderWatcher) =>
        IlluminaProcessingUnitWatcherActor.startIlluminaProcessingUnitWatcherActor()
      case None => parser.showUsageAsError
    }
  } getOrElse {
    // arguments are bad, error message will have been displayed
  }

}