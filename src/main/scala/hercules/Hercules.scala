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
import scala.collection.JavaConversions._
import hercules.protocols.HerculesMainProtocol
import akka.contrib.pattern.ClusterClient.SendToAll
import hercules.actors.demultiplexing.IlluminaDemultiplexingActor

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
  case class CommandLineOptions(applicationType: Option[Command] = None)

  val parser = new scopt.OptionParser[CommandLineOptions]("Hercules") {
    cmd("master") action { (_, c) =>
      c.copy(applicationType = Some(RunMaster))
    }
    cmd("demultiplexer") action { (_, c) =>
      c.copy(applicationType = Some(RunDemultiplexter))
    }
  }

  parser.parse(args, CommandLineOptions()) map { config =>
    // do stuff
    config.applicationType match {
      case Some(RunMaster) =>
        IlluminaDemultiplexingActor.startIlluminaDemultiplexingActor()
      case Some(RunDemultiplexter) =>
        SisyphusMasterActor.startSisyphusMasterActor()
      case None => parser.showUsageAsError
    }
  } getOrElse {
    // arguments are bad, error message will have been displayed
  }

}