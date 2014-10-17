package hercules.actors.demultiplexing

import akka.actor.{ Actor, ActorSystem, PoisonPill, Props }
import akka.event.LoggingReceive
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit, TestProbe }
import hercules.actors.demultiplexing.IlluminaDemultiplexingActor._
import hercules.demultiplexing.Demultiplexer
import hercules.entities.illumina.{ HiSeqProcessingUnit, IlluminaProcessingUnit }
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.demultiplexing.DemultiplexingResult
import hercules.exceptions.HerculesExceptions
import hercules.protocols.HerculesMainProtocol
import hercules.test.utils.StepParent
import java.io.File
import java.io.PrintWriter
import java.net.URI
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import org.scalatest.Assertions.assert
import scala.concurrent.{ duration, Future, ExecutionContext }
import hercules.demultiplexing.DemultiplexingResult

class SisyphusDemultiplexingExecutorActorTest extends TestKit(ActorSystem("SisyphusDemultiplexingExecutorActorTest"))
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers {

  import duration._

  // The processing unit to send that we will return
  val runfolder = new File("runfolder1")
  val processingUnit: IlluminaProcessingUnit =
    new HiSeqProcessingUnit(
      new IlluminaProcessingUnitConfig(
        new File("Samplesheet1"),
        new File("DefaultQC"),
        Some(new File("DefaultProg"))),
      runfolder.toURI)

  val logFile = new File("fake.log")
  val writer = new PrintWriter(logFile)
  val logText = "To be or not to be, that is the question?"
  writer.println(logText)
  writer.close()

  // A fake fetcher class which will just return the processing untis
  // defined above.
  class FakeDemultiplexer(succeed: Boolean, exception: Option[Throwable] = None, block: Duration = Duration.Zero) extends Demultiplexer {
    var cleanUpRan: Boolean = false

    def cleanup(unit: hercules.entities.ProcessingUnit): Unit =
      cleanUpRan = true
    def demultiplex(unit: hercules.entities.ProcessingUnit)(implicit executor: ExecutionContext): Future[hercules.demultiplexing.DemultiplexingResult] = {
      // Block for the specified duration before returning the desired result
      def blockUntil(): Future[DemultiplexingResult] = {
        Thread.sleep(block.toMillis)
        Future(new DemultiplexingResult(unit, succeed, Some(logText)))(executor)
      }

      if (exception.isEmpty) blockUntil()
      else Future.failed(exception.get)
    }
  }

  val parent = TestProbe()
  val successDemuxActor = TestActorRef(
    SisyphusDemultiplexingExecutorActor.props(
      new FakeDemultiplexer(succeed = true)),
    parent.ref,
    "SisyphusDemultiplexingExecutorActor_Success")

  val failDemuxActor = TestActorRef(
    SisyphusDemultiplexingExecutorActor.props(
      new FakeDemultiplexer(succeed = false)),
    parent.ref,
    "SisyphusDemultiplexingExecutorActor_Failure")

  val exceptionText = "Test-case-generated exception"
  val exceptionDemuxActor = TestActorRef(
    SisyphusDemultiplexingExecutorActor.props(
      new FakeDemultiplexer(
        succeed = false,
        exception = Some(HerculesExceptions.ExternalProgramException(exceptionText, processingUnit)))),
    parent.ref,
    "SisyphusDemultiplexingExecutorActor_Exception")

  val blockedDemuxActor = TestActorRef(
    SisyphusDemultiplexingExecutorActor.props(
      new FakeDemultiplexer(
        succeed = true,
        block = 1.minutes
      )).withDispatcher("test.actors.test-dispatcher"),
    "SisyphusDemultiplexingExecutorActor_Blocked")

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    logFile.delete()
    runfolder.delete()
    Thread.sleep(1000)
  }

  "A SisyphusDemultiplexingExecutorActor" should " respond with Idle status when idle" in {

    successDemuxActor ! IlluminaDemultiplexingActorProtocol.RequestExecutorAvailabilityMessage
    expectMsg(3 second, IlluminaDemultiplexingActorProtocol.Idle)
  }

  it should " forward a handled RequestDemultiplexingProcessingUnitMessage to the parent" in {

    successDemuxActor ! HerculesMainProtocol.RequestDemultiplexingProcessingUnitMessage
    parent.expectMsg(3 second, HerculesMainProtocol.RequestDemultiplexingProcessingUnitMessage)

  }

  it should " reject a StartDemultiplexingProcessingUnitMessage if the runfolder could not be found" in {

    runfolder.delete()
    successDemuxActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.Reject(Some(s"The run folder path " + new File(processingUnit.uri) + " could not be found")))

  }

  it should " accept a StartDemultiplexingProcessingUnitMessage if idle and the runfolder can be found" in {

    runfolder.mkdir()
    successDemuxActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.Acknowledge)

  }

  it should " forward success message to parent if the demultiplexing was successful" in {

    parent.expectMsg(3 second, HerculesMainProtocol.FinishedDemultiplexingProcessingUnitMessage(processingUnit))
    successDemuxActor.stop()

  }

  it should " forward a fail message to parent if the demultiplexing failed" in {

    failDemuxActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.Acknowledge)
    parent.expectMsg(3 second, HerculesMainProtocol.FailedDemultiplexingProcessingUnitMessage(processingUnit, logText))
    failDemuxActor.stop()

  }

  it should " send a fail message if the demultiplexing generated an exception" in {

    exceptionDemuxActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.Acknowledge)
    parent.expectMsg(3 second, HerculesMainProtocol.FailedDemultiplexingProcessingUnitMessage(processingUnit, exceptionText))
    exceptionDemuxActor.stop()

  }

  it should " become busy when a demultiplexing job is running" in {

    runfolder.mkdir()
    blockedDemuxActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.Acknowledge)
    blockedDemuxActor ! IlluminaDemultiplexingActorProtocol.RequestExecutorAvailabilityMessage
    expectMsg(3 second, IlluminaDemultiplexingActorProtocol.Busy)

  }

  it should " reject another demultiplex job when busy" in {

    blockedDemuxActor ! HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit)
    expectMsg(3 second, HerculesMainProtocol.Reject(Some("Executor is busy")))

  }

  it should " become idle after demultiplex job has finished" in {

    blockedDemuxActor ! HerculesMainProtocol.FinishedDemultiplexingProcessingUnitMessage(processingUnit)
    parent.expectMsg(3 second, HerculesMainProtocol.FinishedDemultiplexingProcessingUnitMessage(processingUnit))
    blockedDemuxActor ! IlluminaDemultiplexingActorProtocol.RequestExecutorAvailabilityMessage
    expectMsg(3 second, IlluminaDemultiplexingActorProtocol.Idle)
    blockedDemuxActor.stop()
  }

}