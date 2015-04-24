package hercules

import com.typesafe.config.ConfigFactory
import hercules.HerculesStartRoles._
import hercules.actors.demultiplexing.IlluminaDemultiplexingActor
import hercules.actors.masters.SisyphusMasterActor
import hercules.actors.processingunitwatcher.IlluminaProcessingUnitWatcherActor
import hercules.api.RestAPI
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

/**
 * Providing everything that Hercules needs to start up. Not keeping it in the
 * main Hercules object which actually extends App makes for easier testing
 * as this can be extended with a class setting args.
 */
trait HerculesEntryPoint {

  val log = LoggerFactory.getLogger("Hercules")

  /**
   * Convert a string (from the config) to it's corresponding role object
   *
   * @param s String to convert to type
   * @return the corresponding role
   */
  def string2Role(s: String): Role = {
    s match {
      case "master"        => RunMaster()
      case "demultiplexer" => RunDemultiplexer()
      case "watcher"       => RunRunfolderWatcher()
      case "restapi"       => RestApi()
    }
  }

  val parser = new scopt.OptionParser[CommandLineOptions]("Hercules") {

    cmd("help") action { (_, c) =>
      c.copy(applicationType = Some(List(RunHelp())))
    }

    cmd("master") action { (_, c) =>
      c.copy(applicationType = Some(List(RunMaster())))
    }

    cmd("demultiplexer") action { (_, c) =>
      c.copy(applicationType = Some(List(RunDemultiplexer())))
    }

    cmd("watcher") action { (_, c) =>
      c.copy(applicationType = Some(List(RunRunfolderWatcher())))
    }

    cmd("restapi") action { (_, c) =>
      c.copy(applicationType = Some(List(RestApi())))
    }
  }

  /**
   * Load the default configuration from the application.conf
   * @param config with no defined CommandLineOptions
   * @return the default CommandLineOptions
   */
  def loadDefaultCommandLineConfig(config: CommandLineOptions): CommandLineOptions = {

    require(config.applicationType == None)

    val configLoader = ConfigFactory.load()
    val rolesToStart = configLoader.getStringList("hercules.roles").toList

    log.info(
      "When Hercules starts without any arguments it will by default " +
        "read the list roles from the application.conf. Will now attempt to " +
        "start the following roles: ")
    rolesToStart.foreach(log.info(_))

    val mappedToRoles = rolesToStart.map(x => string2Role(x))

    assert(!mappedToRoles.isEmpty)

    config.copy(applicationType = Some(mappedToRoles))
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
        parseCommandLineOptions(
          loadDefaultCommandLineConfig(config))
      }
      case Some(list) => {
        list.foreach { x =>
          x match {
            case RunHelp() =>
              parser.showUsage
            case RunMaster() =>
              SisyphusMasterActor.startSisyphusMasterActor()
            case RunDemultiplexer() =>
              IlluminaDemultiplexingActor.startIlluminaDemultiplexingActor()
            case RunRunfolderWatcher() =>
              IlluminaProcessingUnitWatcherActor.startIlluminaProcessingUnitWatcherActor()
            case RestApi() =>
              RestAPI
          }
        }
      }
    }
  }
}