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
import com.typesafe.config.Config
import akka.contrib.pattern.ClusterReceptionistExtension
import scala.concurrent.duration._
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.japi.Util.immutableSeq
import akka.contrib.pattern.ClusterReceptionist
import hercules.test.utils.FakeMaster

class IlluminaProcessingUnitWatcherActorTest
    extends TestKit(
      ActorSystem(
        "ClusterSystem",
        ConfigFactory.parseString("""akka.remote.netty.tcp.port=1337""").
          withFallback(ConfigFactory.parseString("""remote.netty.tcp.hostname=127.0.0.1""")).
          withFallback(ConfigFactory.parseString("""seed-nodes = ["akka.tcp://ClusterSystem@127.0.0.1:1337"]""")).
          withFallback(ConfigFactory.load())))
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers {

  import HerculesMainProtocol._
  val generalConfig = ConfigFactory.load()

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

  object FakeExecutor {
    def props(
      success: Boolean = true,
      exception: Option[Exception] = None,
      startTest: Boolean = true): Props = {
      Props(new FakeExecutor(success, exception, startTest))
    }
  }

  class FakeExecutor(
      val success: Boolean,
      val exception: Option[Exception],
      val startTest: Boolean) extends Actor with ActorLogging {

    import context.dispatcher
    if (startTest) {
      context.system.scheduler.scheduleOnce(1 second, self, "StartTest")
    }

    def receive() = {
      case "StartTest" => {
        log.info("FakeExecutor trying to send message to the clusterClient")
        for (unit <- processingUnits)
          context.parent ! FoundProcessingUnitMessage(unit)
      }
      case ForgetProcessingUnitMessage(unit) => {
        if (exception.nonEmpty) sender ! Reject(Some("Executor encountered exception " + exception.get.getMessage))
        else {
          if (success) sender ! Acknowledge
          else sender ! Reject(Some("Testing failure"))
        }
      }
    }
  }

  val hostname = "127.0.0.1"
  val port = 2551
  val user = "ClusterSystem"
  val contact = s"akka.tcp://$user@$hostname:$port"
  val masterSystem: ActorSystem = {
    val config =
      ConfigFactory.
        parseString(
          s"""
    		akka {
    			remote.netty.tcp.port=$port
    			remote.netty.tcp.hostname=$hostname  
    			cluster.roles=["master"]
    
    			cluster {
    				seed-nodes = ["$contact"]
    				auto-down-unreachable-after = 10s
    			}
    		}   
    		contact-points = ["$contact"]            
            """).
          withFallback(generalConfig)
    ActorSystem("ClusterSystem", config)
  }

  // Create a fake master
  masterSystem.actorOf(
    ClusterSingletonManager.props(
      FakeMaster.props(testActor),
      "active",
      PoisonPill,
      Some("master")),
    "master")

  val initialContacts = List(contact).map {
    case AddressFromURIString(addr) => masterSystem.actorSelection(RootActorPath(addr) / "user" / "receptionist")
  }.toSet

  masterSystem.actorOf(ClusterClient.props(initialContacts), "clusterClient")
  val clusterClient = masterSystem.actorOf(ClusterClient.props(initialContacts))

  val watcherPort = port + 1
  val watcherConfig =
    ConfigFactory.
      parseString(
        s"""
            remote.netty.tcp.port=$watcherPort
            remote.netty.tcp.hostname=$hostname
            """).
        withFallback(generalConfig)

  val defaultWatcher = IlluminaProcessingUnitWatcherActor.
    startIlluminaProcessingUnitWatcherActor(
      system = masterSystem,
      executor = FakeExecutor.props(),
      clusterClientCustomConfig = () => watcherConfig,
      getClusterClient = (_, _) => clusterClient)

  override def afterAll(): Unit = {
    system.shutdown()
    masterSystem.shutdown()
    Thread.sleep(1000)
  }

  "A IlluminaProcessingUnitWatcherActor" should " pass any FoundProcessingUnitMessage on to the master" in {

    within(10.seconds) {
      expectMsg(FakeMaster.MasterWrapped(FoundProcessingUnitMessage(processingUnits(0))))
      expectMsg(FakeMaster.MasterWrapped(FoundProcessingUnitMessage(processingUnits(1))))
    }
  }

  it should "pass a RequestProcessingUnitMessage on to the master" in {
    defaultWatcher ! RequestProcessingUnitMessage
    expectMsg(3.seconds, FakeMaster.MasterWrapped(RequestProcessingUnitMessage))
  }

  it should "pass a ForgetProcessingUnitMessage to the executer and pipe the successful result back to the master" in {
    defaultWatcher ! ForgetProcessingUnitMessage(processingUnits(0))
    expectMsg(3.seconds, FakeMaster.MasterWrapped(Acknowledge))
    defaultWatcher ! PoisonPill
  }
  it should "pass a ForgetProcessingUnitMessage to the executer and pipe the failing result back to the master" in {
    val failingWatcher =
      masterSystem.actorOf(
        IlluminaProcessingUnitWatcherActor.props(
          clusterClient,
          FakeExecutor.props(success = false, startTest = false)),
        "IlluminaProcessingUnitWatcherActor_Failing")
    failingWatcher ! ForgetProcessingUnitMessage(processingUnits(0))
    expectMsg(3.seconds, FakeMaster.MasterWrapped(Reject(Some("Testing failure"))))
    failingWatcher ! PoisonPill
  }
  it should "pass a ForgetProcessingUnitMessage to the executer and handle a thrown exception and pipe the result back to the master" in {
    val exception = new Exception("Testing exception")
    val exceptionWatcher =
      masterSystem.actorOf(
        IlluminaProcessingUnitWatcherActor.props(
          clusterClient,
          FakeExecutor.props(exception = Some(exception), startTest = false)),
        "IlluminaProcessingUnitWatcherActor_Exception")
    exceptionWatcher ! ForgetProcessingUnitMessage(processingUnits(0))
    expectMsg(3.seconds, FakeMaster.MasterWrapped(Reject(Some("Executor encountered exception " + exception.getMessage))))
    exceptionWatcher ! PoisonPill
  }

}