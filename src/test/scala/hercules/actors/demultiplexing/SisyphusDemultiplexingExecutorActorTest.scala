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
import HerculesMainProtocol._
import hercules.test.utils.StepParent

class SisyphusDemultiplexingExecutorActorTest(_system: ActorSystem) extends TestKit(_system)
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers {

  import duration._

  def this() = this(ActorSystem("SisyphusDemultiplexingExecutorActorTest"))

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

  val externalProgramFailedReason = "External program failed!"

  // A fake fetcher class which will just return the processing untis
  // defined above.
  class FakeDemultiplexer(succeed: Boolean, exception: Option[Throwable] = None, block: Duration = Duration.Zero) extends Demultiplexer {
    var cleanUpRan: Boolean = false

    import ExecutionContext.Implicits.global

    def cleanup(unit: hercules.entities.ProcessingUnit): Unit =
      cleanUpRan = true

    def demultiplex(unit: hercules.entities.ProcessingUnit)(implicit executor: ExecutionContext): Future[hercules.demultiplexing.DemultiplexingResult] = {
      // Block for the specified duration before returning the desired result
      def blockUntil(): Future[DemultiplexingResult] = {
        Thread.sleep(block.toMillis)
        Future(new DemultiplexingResult(unit, succeed, Some(logText)))
      }

      if (exception.isEmpty) blockUntil()
      else Future.failed(exception.get)
    }
  }

  val successfullDemultiplexer = new FakeDemultiplexer(succeed = true)
  val unsuccessfullDemultiplexer = new FakeDemultiplexer(succeed = false)
  val exceptionDemultiplexer =
    new FakeDemultiplexer(
      succeed = false,
      exception =
        Some(new HerculesExceptions.ExternalProgramException(externalProgramFailedReason, processingUnit)))

  val proxy = TestProbe()
  def parent(demultiplexer: Demultiplexer) = system.actorOf(Props(new Actor {
    val child = context.actorOf(SisyphusDemultiplexingExecutorActor.props(demultiplexer), "child")
    def receive = {
      case x if sender == child => proxy.ref forward x
      case x                    => child forward x
    }
  }))

  val parentWithGoodExecutor = parent(successfullDemultiplexer)
  val parentWithBadExecutor = parent(unsuccessfullDemultiplexer)
  val parentWithExceptionExecutor = parent(exceptionDemultiplexer)

  runfolder.mkdir()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    logFile.delete()
    runfolder.delete()
  }

  "A SisyphusDemultiplexingExecutorActor" should " start demultiplexing when given a StartDemultiplexingProcessingUnitMessage" in {
    proxy.send(parentWithGoodExecutor, StartDemultiplexingProcessingUnitMessage(processingUnit))
    proxy.expectMsg(3.seconds, Acknowledge)
    proxy.expectMsg(3.seconds, FinishedDemultiplexingProcessingUnitMessage(processingUnit))
  }

  it should "reject if the processing unit cannot be found" in {
    runfolder.delete()
    proxy.send(parentWithGoodExecutor, StartDemultiplexingProcessingUnitMessage(processingUnit))
    proxy.expectMsg(3.seconds, Reject(Some("The run folder path " + runfolder.getAbsolutePath() + " could not be found")))
    runfolder.mkdir()
  }

  it should "send a FailedDemultiplexingProcessingUnitMessage if demultiplexing failes" in {
    proxy.send(parentWithBadExecutor, StartDemultiplexingProcessingUnitMessage(processingUnit))
    proxy.expectMsg(3.seconds, Acknowledge)
    proxy.expectMsg(3.seconds, FailedDemultiplexingProcessingUnitMessage(processingUnit, reason = logText))
  }

  it should "send a FailedDemultiplexingProcessingUnitMessage if an exception is thrown by the Demultiplexer" in {
    proxy.send(parentWithExceptionExecutor, StartDemultiplexingProcessingUnitMessage(processingUnit))
    proxy.expectMsg(3.seconds, Acknowledge)
    proxy.expectMsg(3.seconds, FailedDemultiplexingProcessingUnitMessage(processingUnit, reason = externalProgramFailedReason))
  }
}
