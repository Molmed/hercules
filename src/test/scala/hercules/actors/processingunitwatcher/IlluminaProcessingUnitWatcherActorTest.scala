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
    def props(): Props = {
      Props(new FakeExecutor())
    }
  }

  class FakeExecutor() extends Actor with ActorLogging {

    import context.dispatcher
    context.system.scheduler.scheduleOnce(1 second, self, "StartTest")

    def receive() = {
      case "StartTest" => {
        log.info("FakeExecutor trying to send message to the clusterClient")
        for (unit <- processingUnits)
          context.parent ! HerculesMainProtocol.FoundProcessingUnitMessage(unit)
      }
    }
  }

  val masterSystem: ActorSystem = {
    val config =
      ConfigFactory.
        parseString(
          """
    		akka {
    			remote.netty.tcp.port=2551
    			remote.netty.tcp.hostname=127.0.0.1  
    			cluster.roles=["master"]
    
    			cluster {
    				seed-nodes = ["akka.tcp://ClusterSystem@127.0.0.1:2551"]
    				auto-down-unreachable-after = 10s
    			}
    		}   
    		contact-points = ["akka.tcp://ClusterSystem@127.0.0.1:2551"]            
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

  override def afterAll(): Unit = {
    system.shutdown()
    masterSystem.shutdown()
    Thread.sleep(1000)
  }

  "A IlluminaProcessingUnitWatcherActor" should " pass any FoundProcessingUnitMessage on to the master" in {

    val initialContacts = List("akka.tcp://ClusterSystem@127.0.0.1:2551").map {
      case AddressFromURIString(addr) ⇒ masterSystem.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    masterSystem.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    val clusterClient = masterSystem.actorOf(ClusterClient.props(initialContacts))

    val watcherConfig =
      ConfigFactory.
        parseString(
          """
      		remote.netty.tcp.port=2552
			remote.netty.tcp.hostname=127.0.0.1  
            """).
          withFallback(generalConfig)

    val fakeExecutor = FakeExecutor.props()

    val watcher = IlluminaProcessingUnitWatcherActor.
      startIlluminaProcessingUnitWatcherActor(
        system = masterSystem,
        executor = fakeExecutor,
        clusterClientCustomConfig = () => watcherConfig,
        getClusterClient = (_, _) => clusterClient)

    within(10.seconds) {
      expectMsg(FakeMaster.MasterWrapped(HerculesMainProtocol.FoundProcessingUnitMessage(processingUnits(0))))
      expectMsg(FakeMaster.MasterWrapped(HerculesMainProtocol.FoundProcessingUnitMessage(processingUnits(1))))
    }
  }
}