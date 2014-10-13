package hercules.actors.demultiplexing

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import hercules.demultiplexing.Demultiplexer
import hercules.entities.illumina.HiSeqProcessingUnit
import hercules.entities.illumina.IlluminaProcessingUnit
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import java.io.File
import java.net.URI
import akka.actor.Props
import hercules.test.utils.StepParent
import hercules.demultiplexing.DemultiplexingResult
import akka.actor.PoisonPill
import hercules.protocols.HerculesMainProtocol
import scala.concurrent.{ duration, Future, ExecutionContext }
import java.io.PrintWriter

class SisyphusDemultiplexingExecutorActorTest extends TestKit(ActorSystem("SisyphusDemultiplexingExecutorActorTest"))
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers {

  import duration._

  // The processing unit to send that we will return
  val processingUnit: IlluminaProcessingUnit =
    new HiSeqProcessingUnit(
      new IlluminaProcessingUnitConfig(
        new File("Samplesheet1"),
        new File("DefaultQC"),
        Some(new File("DefaultProg"))),
      new URI("/path/to/runfolder1"))

  val logFile = new File("fake.log")
  val writer = new PrintWriter(logFile)
  val logText = "To be or not to be, that is the question?"
  writer.println(logText)
  writer.close()

  // A fake fetcher class which will just return the processing untis
  // defined above.
  class FakeDemultiplexer(succeed: Boolean, exception: Option[Throwable] = None) extends Demultiplexer {
    var cleanUpRan: Boolean = false

    def cleanup(unit: hercules.entities.ProcessingUnit): Unit =
      cleanUpRan = true
    def demultiplex(unit: hercules.entities.ProcessingUnit)(implicit executor: ExecutionContext): Future[hercules.demultiplexing.DemultiplexingResult] =
      if (exception.isEmpty) Future.successful(new DemultiplexingResult(succeed, Some(logText)))
      else Future.failed(exception.get)
  }

  override def afterAll(): Unit = {
    system.shutdown()
    logFile.delete()
    Thread.sleep(1000)
  }

  "A SisyphusDemultiplexingExecutorActor" should " send success message if the demultiplexing was successful" in {

    val demultiplexer = new FakeDemultiplexer(succeed = true)

    val demultiplexerActor = system.actorOf(
      SisyphusDemultiplexingExecutorActor.props(demultiplexer))

    demultiplexerActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.FinishedDemultiplexingProcessingUnitMessage(processingUnit))
    demultiplexerActor ! PoisonPill
  }

  it should "send a fail message if the demultiplexing failed " in {

    val demultiplexer = new FakeDemultiplexer(succeed = false)

    val demultiplexerActor = system.actorOf(
      SisyphusDemultiplexingExecutorActor.props(demultiplexer))

    demultiplexerActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.FailedDemultiplexingProcessingUnitMessage(processingUnit, logText))
    demultiplexerActor ! PoisonPill

  }

  it should "send a fail message if the demultiplexing generated an exception " in {

    val demultiplexer = new FakeDemultiplexer(
      succeed = false,
      exception = Some(new Exception("Test-case-generated exception")))

    val demultiplexerActor = system.actorOf(
      SisyphusDemultiplexingExecutorActor.props(demultiplexer))

    demultiplexerActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.FailedDemultiplexingProcessingUnitMessage(processingUnit, logText))
    demultiplexerActor ! PoisonPill

  }
}