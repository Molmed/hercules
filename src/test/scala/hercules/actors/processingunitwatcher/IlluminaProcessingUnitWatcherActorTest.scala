package hercules.actors.processingunitwatcher

import java.io.File
import java.net.URI

import scala.collection.JavaConversions.asScalaBuffer

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers

import com.typesafe.config.ConfigFactory

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.AddressFromURIString
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.RootActorPath
import akka.actor.actorRef2Scala
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterSingletonManager
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.entities.illumina.HiSeqProcessingUnit
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.protocols.HerculesMainProtocol

  //extends TestKit(ActorSystem("IlluminaProcessingUnitWatcherActorTest"))
class IlluminaProcessingUnitWatcherActorTest(_system: ActorSystem) 
    extends TestKit(_system)
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers {

  val generalConfig = ConfigFactory.load()
  val defaultConfig = generalConfig.getConfig("master").
    withFallback(ConfigFactory.parseString("master.akka.remote.netty.tcp.hostname=localhost")).
    withFallback(ConfigFactory.parseString("""master.akka.cluster.roles=["master"]""")).
    withFallback(ConfigFactory.parseString("""master.contact-points=["akka.tcp://ClusterSystem@localhost:2551"]""")).
    withFallback(ConfigFactory.parseString("""master.akka.cluster.seed-nodes = ["akka.tcp://ClusterSystem@localhost:2551"]""")).
    withFallback(generalConfig)

  // The processing units that we will return
  val processingUnits: Seq[IlluminaProcessingUnit] = Seq(
    new HiSeqProcessingUnit(
      new IlluminaProcessingUnitConfig(
        new File("Samplesheet1"),
        new File("DefaultQC"),
        Some(new File("DefaultProg"))),
      new URI("/path/to/runfolder1")),
    new HiSeqProcessingUnit(
      new IlluminaProcessingUnitConfig(
        new File("Samplesheet2"),
        new File("DefaultQC"),
        Some(new File("DefaultProg"))),
      new URI("/path/to/runfolder2")))

  object FakeMaster {

    def props(testProbe: ActorRef): Props = Props(new FakeMaster(testProbe))

  }

  class FakeMaster(testProbe: ActorRef) extends Actor with ActorLogging {
    // The master will register it self to the cluster receptionist.
    // ClusterReceptionistExtension(context.system).registerService(self)

    def receive() = {
      case msg => testProbe.tell(msg, sender)
    }
  }

  object FakeExecutor {
    def props(): Props = {
      Props(new FakeExecutor)
    }
  }

  class FakeExecutor extends Actor with ActorLogging {
    //@TODO 
    // This needs to push a message up to the parent after starting.

    def receive() = {
      case _ => log.info("FakeExecutor got message. Ignore it.")
    }
  }

  val masterSystem: ActorSystem = {
    val config = ConfigFactory.parseString("akka.cluster.roles=[master]").withFallback(defaultConfig)
    ActorSystem("IlluminaProcessingUnitWatcherActorTest", config)
  }

  // Initiate cluster systems    
  val clusterSystem = ActorSystem("ClusterSystem", defaultConfig)

  val clusterAdress = Cluster(masterSystem).selfAddress
  Cluster(system).join(clusterAdress)

  // Create a fake master
  masterSystem.actorOf(
    ClusterSingletonManager.props(
      FakeMaster.props(testActor),
      "active",
      PoisonPill,
      Some("master")),
    "master")

  val initialContacts = defaultConfig.getStringList("master.contact-points").map {
    case AddressFromURIString(addr) ⇒ system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
  }.toSet

  val clusterClient = system.actorOf(ClusterClient.props(initialContacts))

  override def afterAll(): Unit = {
    system.shutdown()
    masterSystem.shutdown()
    Thread.sleep(1000)
  }

  "A IlluminaProcessingUnitWatcherActor" should " pass any FoundProcessingUnitMessage on to the master" in {
    
    // Create the watcher with a fake executor
    val watcher = clusterSystem.actorOf(IlluminaProcessingUnitWatcherActor.props(clusterClient, FakeExecutor.props()))

    // Check that it gets it processes messages correctly
    watcher ! HerculesMainProtocol.FoundProcessingUnitMessage(processingUnits(0))
    expectMsg(HerculesMainProtocol.FoundProcessingUnitMessage(processingUnits(0)))

    watcher ! HerculesMainProtocol.FoundProcessingUnitMessage(processingUnits(1))
    expectMsg(HerculesMainProtocol.FoundProcessingUnitMessage(processingUnits(1)))

  }

}