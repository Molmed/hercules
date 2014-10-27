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
import akka.actor.PoisonPill
import akka.actor.Props
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import hercules.config.processing.IlluminaProcessingUnitWatcherConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.config.processingunit.IlluminaProcessingUnitFetcherConfig
import hercules.entities.illumina.HiSeqProcessingUnit
import hercules.entities.ProcessingUnit
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.entities.illumina.IlluminaProcessingUnitFetcher
import hercules.protocols.HerculesMainProtocol
import hercules.test.utils.StepParent

class IlluminaProcessingUnitWatcherExecutorActorTest
    extends TestKit(ActorSystem("IlluminaProcessingUnitExecutorActorTest"))
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers {

  import HerculesMainProtocol._

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
  class FakeFetcher(returnUnit: Option[IlluminaProcessingUnit] = None, exception: Option[Exception] = None) extends IlluminaProcessingUnitFetcher {
    override def checkForReadyProcessingUnits(
      config: IlluminaProcessingUnitFetcherConfig): Seq[IlluminaProcessingUnit] = {
      processingUnits
    }
    override def searchForProcessingUnitName(
      unitName: String,
      config: IlluminaProcessingUnitFetcherConfig): Option[IlluminaProcessingUnit] = {
      if (exception.nonEmpty) throw exception.get
      else returnUnit
    }
  }

  class FakeProcessingUnit(behavior: Boolean => Boolean = (x: Boolean) => x) extends IlluminaProcessingUnit {
    val uri = new File("FakeTestUnit").toURI
    val processingUnitConfig = IlluminaProcessingUnitConfig(new File(name), new File(name), None)
    var discoveredState = true
    override def isFound: Boolean = behavior(discoveredState)
    override def markAsFound: Boolean = { discoveredState = true; true }
    override def markNotFound: Boolean = { discoveredState = false; true }
  }

  val fetcher = new FakeFetcher
  val parent = TestProbe()

  // Use this function to by pass the default configuration.
  def getTestConfig(): IlluminaProcessingUnitWatcherConfig = {
    val config = new IlluminaProcessingUnitWatcherConfig(
      List("runfolder"),
      "samplesheet",
      "customQcConfigRoot",
      "defaultQcConfig",
      "customProgamConfigurationRoot",
      "defaultProgramConfigurationFile",
      1)

    config
  }

  override def afterAll(): Unit = {
    system.shutdown()
    Thread.sleep(1000)
  }

  "A IlluminaProcessingUnitWatcherExecutorActor" should "load correct default configs" in {
    val expected =
      new IlluminaProcessingUnitWatcherConfig(List("/seqdata/biotank1/runfolders/"),
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
      expectMsg(3.second, FoundProcessingUnitMessage(unit))
    watcher ! PoisonPill
  }

  it should "respond to a ForgetProcessingUnitMessage with Reject to parent if the unit could not be found" in {
    val watcher = TestActorRef(
      IlluminaProcessingUnitWatcherExecutorActor.props(fetcher, getTestConfig),
      parent.ref,
      "IlluminaProcessingUnitWatcherExecutorActor_NotFound")
    val unitName = "testUnit"
    watcher ! ForgetProcessingUnitMessage(unitName)
    parent.expectMsg(3.second, Reject(Some(s"Could not locate ProcessingUnit corresponding to $unitName")))
    watcher.stop()
  }

  it should "respond to a ForgetProcessingUnitMessage with Reject to parent if an exception occurred" in {
    val exception = new Exception("Testing exception")
    val watcher = TestActorRef(
      IlluminaProcessingUnitWatcherExecutorActor.props(
        new FakeFetcher(exception = Some(exception)),
        getTestConfig),
      parent.ref,
      "IlluminaProcessingUnitWatcherExecutorActor_NotFound")
    val unitName = "testUnit"
    watcher ! ForgetProcessingUnitMessage(unitName)
    parent.expectMsg(3.second, Reject(Some("IlluminaProcessingUnitWatcherExecutorActor encountered exception while processing ForgetProcessingUnitMessage: " + exception.getMessage)))
    watcher.stop()
  }

  it should "respond to a ForgetProcessingUnitMessage with Reject to parent if the unit could be found but not forgotten" in {
    val watcher = TestActorRef(
      IlluminaProcessingUnitWatcherExecutorActor.props(
        new FakeFetcher(returnUnit = Some(new FakeProcessingUnit((x: Boolean) => true))),
        getTestConfig),
      parent.ref,
      "IlluminaProcessingUnitWatcherExecutorActor_NotFound")
    val unitName = "testUnit"
    watcher ! ForgetProcessingUnitMessage(unitName)
    parent.expectMsg(3.second, Reject(Some(s"ProcessingUnit corresponding to $unitName was found but could not be undiscovered")))
    watcher.stop()
  }

  it should "respond to a ForgetProcessingUnitMessage with Acknowledge to parent if the unit could be found and undiscovered" in {
    val watcher = TestActorRef(
      IlluminaProcessingUnitWatcherExecutorActor.props(
        new FakeFetcher(returnUnit = Some(new FakeProcessingUnit())),
        getTestConfig),
      parent.ref,
      "IlluminaProcessingUnitWatcherExecutorActor_NotFound")
    val unitName = "testUnit"
    watcher ! ForgetProcessingUnitMessage(unitName)
    parent.expectMsg(3.second, Acknowledge)
    watcher.stop()
  }

}