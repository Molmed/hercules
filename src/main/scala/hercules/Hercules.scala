package hercules

import hercules.actors.demultiplexing.IlluminaDemultiplexingActor
import hercules.actors.masters.SisyphusMasterActor
import hercules.actors.processingunitwatcher.IlluminaProcessingUnitWatcherActor
import hercules.api.RestAPI
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import akka.dispatch.Foreach
import scala.collection.JavaConversions._
import akka.event.Logging
import org.slf4j.LoggerFactory

/**
 * The main entry point for the application
 *
 * Will parse the command line options and initiate the appropriate
 * system depending on the command and options passed.
 */
object Hercules extends App {

  val log = LoggerFactory.getLogger("Hercules")

  /**
   * The base trait used for all roles to run.
   */
  sealed trait Role

  /**
   * Run the master role
   */
  case object RunMaster extends Role

  /**
   * Run the demultiplexer role
   */
  case object RunDemultiplexer extends Role

  /**
   * Run the watcher role
   */
  case object RunRunfolderWatcher extends Role

  /**
   * Run the REST API role
   */
  case object RestApi extends Role

  /**
   * Output the help message
   */
  case object RunHelp extends Role

  /**
   * Convert a string (from the config) to it's corresponding role object
   *
   * @param s String to convert to type
   * @return the corresponding role
   */
  def string2Role(s: String): Role = {
    s match {
      case "master"        => RunMaster
      case "demultiplexer" => RunDemultiplexer
      case "watcher"       => RunRunfolderWatcher
      case "restapi"       => RestApi
    }
  }

  /**
   * Container class for command line options
   * @param applicationType
   */
  case class CommandLineOptions(
    applicationType: Option[List[Role]] = None)

  val parser = new scopt.OptionParser[CommandLineOptions]("Hercules") {

    cmd("help") action { (_, c) =>
      c.copy(applicationType = Some(List(RunHelp)))
    }

    cmd("master") action { (_, c) =>
      c.copy(applicationType = Some(List(RunMaster)))
    }

    cmd("demultiplexer") action { (_, c) =>
      c.copy(applicationType = Some(List(RunDemultiplexer)))
    }

    cmd("watcher") action { (_, c) =>
      c.copy(applicationType = Some(List(RunRunfolderWatcher)))
    }

    cmd("restapi") action { (_, c) =>
      c.copy(applicationType = Some(List(RestApi)))
    }
  }

  /**
   * Parse the command line options.
   *
   * If none (config.applicationType is None) are given, the default options
   * will be read from the application config file.
   *
   * @param config a CommandLineOptions instance
   * @return Unit
   */
  def parseCommandLineOptions(config: CommandLineOptions): Unit = {

    config.applicationType match {
      // The default case is to try to get the roles from the application.conf
      case None => {
        val configLoader = ConfigFactory.load()
        val rolesToStart = configLoader.getStringList("hercules.roles").toList

        log.info(
          "When Hercules starts without any arguments it will by default " +
            "read the list roles from the application.conf. Will now attempt to " +
            "start the following roles: ")
        rolesToStart.foreach(log.info(_))

        val mappedToRoles = rolesToStart.map(x => string2Role(x))
        val updatedConfig = config.copy(applicationType = Some(mappedToRoles))
        parseCommandLineOptions(updatedConfig)
      }
      case Some(list) => {
        list.foreach { x =>
          x match {
            case RunHelp =>
              parser.showUsage
            case RunMaster =>
              SisyphusMasterActor.startSisyphusMasterActor()
            case RunDemultiplexer =>
              IlluminaDemultiplexingActor.startIlluminaDemultiplexingActor()
            case RunRunfolderWatcher =>
              IlluminaProcessingUnitWatcherActor.startIlluminaProcessingUnitWatcherActor()
            case RestApi =>
              RestAPI
          }
        }
      }
    }
  }

  parser.parse(args, CommandLineOptions()) map { config =>
    // do stuff
    parseCommandLineOptions(config)
  }
}