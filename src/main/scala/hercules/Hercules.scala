package hercules

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.RootActorPath
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterSingletonManager
import akka.japi.Util.immutableSeq
import akka.actor.AddressFromURIString
import akka.actor.ActorPath
import akka.persistence.journal.leveldb.SharedLeveldbStore
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.Identify
import akka.actor.ActorIdentity
import akka.persistence.journal.leveldb.SharedLeveldbJournal
import hercules.actors.masters.SisyphusMasterActor
import hercules.actors.notifiers.EmailNotifierActor
import scala.collection.JavaConversions._
import hercules.protocols.HerculesMainProtocol
import akka.contrib.pattern.ClusterClient.SendToAll
import hercules.actors.demultiplexing.IlluminaDemultiplexingActor
import hercules.actors.masters.SisyphusMasterActor
import hercules.actors.processingunitwatcher.IlluminaProcessingUnitWatcherActor
import hercules.actors.interactive.InteractiveActor
import hercules.actors.notifiers.EmailNotifierActor

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
  case object RunInteractive extends Command
  case object RunNotifier extends Command

  case class CommandLineOptions(
    applicationType: Option[Command] = None,
    command: Option[String] = None,
    unitName: Option[String] = None)

  val parser = new scopt.OptionParser[CommandLineOptions]("Hercules") {

    cmd("master") action { (_, c) =>
      c.copy(applicationType = Some(RunMaster))
    }

    cmd("demultiplexer") action { (_, c) =>
      c.copy(applicationType = Some(RunDemultiplexter))
    }

    cmd("notifier") action { (_, c) =>
      c.copy(applicationType = Some(RunNotifier))
    }

    cmd("watcher") action { (_, c) =>
      c.copy(applicationType = Some(RunRunfolderWatcher))
    }

    cmd("interactive") action { (_, c) =>
      c.copy(applicationType = Some(RunInteractive))
    } children (
      cmd("restart") action{(_, c) => c.copy(command = Some("restart"))} children (
        arg[String]("unit") required () action { (x, c) =>
          c.copy(unitName = Some(x))
        }))
        
    cmd("notifier") action { (_, c) =>
      c.copy(applicationType = Some(RunNotifier))
    }
    
  }

  parser.parse(args, CommandLineOptions()) map { config =>
    // do stuff
    config.applicationType match {
      case Some(RunMaster) =>
        SisyphusMasterActor.startSisyphusMasterActor()
      case Some(RunNotifier) => 
        EmailNotifierActor.startEmailNotifierActor()
      case Some(RunDemultiplexter) =>
        IlluminaDemultiplexingActor.startIlluminaDemultiplexingActor()
      case Some(RunRunfolderWatcher) =>
        IlluminaProcessingUnitWatcherActor.startIlluminaProcessingUnitWatcherActor()
      case Some(RunInteractive) =>
        InteractiveActor.startInteractive(config.command.get, config.unitName.get)
      case None => parser.showUsageAsError
    }
  } getOrElse {
    // arguments are bad, error message will have been displayed
  }

}