package hercules.actors.processingunitwatcher

import java.io.File
import java.net.URI

import scala.concurrent.duration.DurationInt

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import hercules.config.processing.IlluminaProcessingUnitWatcherConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.config.processingunit.IlluminaProcessingUnitFetcherConfig
import hercules.entities.illumina.HiSeqProcessingUnit
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.entities.illumina.IlluminaProcessingUnitFetcher
import hercules.protocols.HerculesMainProtocol

class IlluminaProcessingUnitWatcherExecutorActorTest
    extends TestKit(ActorSystem("IlluminaProcessingUnitExecutorActorTest"))
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers {

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

  // A fake fetcher class which will just return the processing untis
  // defined above.
  class FakeFetcher extends IlluminaProcessingUnitFetcher {
    override def checkForReadyProcessingUnits(
      config: IlluminaProcessingUnitFetcherConfig): Seq[IlluminaProcessingUnit] = {
      processingUnits
    }
  }

  val fetcher = new FakeFetcher

  // Use this function to by pass the default configuration.
  def getTestConfig(): IlluminaProcessingUnitWatcherConfig = {
    val config = new IlluminaProcessingUnitWatcherConfig(
      "runfolder",
      "samplesheet",
      "customQcConfigRoot",
      "defaultQcConfig",
      "customProgamConfigurationRoot",
      "defaultProgramConfigurationFile",
      1)

    config
  }

  /**
   * Use this class to create a fake parent class to pass messages through
   * which should have been sent to the master.
   * It will simply take any messages from the child and forward them.
   */
  class StepParent(childToCreate: Props, probe: ActorRef) extends Actor {
    val child = context.actorOf(childToCreate, "child")
    def receive = {
      case msg => probe.tell(msg, sender)
    }
  }

  override def afterAll(): Unit = {
    system.shutdown()
    Thread.sleep(1000)
  }

  "A IlluminaProcessingUnitWatcherExecutorActor" should "load correct default configs" in {
    val expected =
      new IlluminaProcessingUnitWatcherConfig("/seqdata/biotank1/runfolders/",
        "/srv/samplesheet/processning/",
        "/srv/qc_config/custom/",
        "/srv/qc_config/sisyphus_qc.xml",
        "/srv/program_config/custom/",
        "/srv/program_config/sisyphus.yml",
        5)
    val config = IlluminaProcessingUnitWatcherExecutorActor.createDefaultConfig
    assert(expected === config)
  }

  it should " be able get runfolders from the fetcher and pass them on to the parent" in {
    val watcher = system.actorOf(
      Props(
        new StepParent(IlluminaProcessingUnitWatcherExecutorActor.props(fetcher, getTestConfig),
          testActor)))

    for (unit <- processingUnits)
      expectMsg(3.second, HerculesMainProtocol.FoundProcessingUnitMessage(unit))
  }
}