package hercules.actors.demultiplexing

import akka.actor.{ Actor, ActorSystem, PoisonPill, Props }
import akka.event.LoggingReceive

import hercules.demultiplexing.Demultiplexer
import hercules.entities.illumina.{ HiSeqProcessingUnit, IlluminaProcessingUnit }
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.demultiplexing.DemultiplexingResult
import hercules.exceptions.HerculesExceptions
import hercules.protocols.HerculesMainProtocol
import hercules.actors.demultiplexing.IlluminaDemultiplexingActor._

import java.io.File
import java.io.PrintWriter
import java.net.URI

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import org.scalatest.Assertions.assert

import scala.concurrent.{ duration, Future, ExecutionContext }

class SisyphusDemultiplexingExecutorActorHotswappingTest
    extends FlatSpecLike
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

  implicit val system = ActorSystem("SisyphusDemultiplexingExecutorActorHotswappingTest")
  implicit val executor = system.dispatcher

  // A fake fetcher class which will just return the processing untis
  // defined above.
  class FakeDemultiplexer(succeed: Boolean, exception: Option[Throwable] = None, block: Duration = Duration.Zero) extends Demultiplexer {
    var cleanUpRan: Boolean = false

    def cleanup(unit: hercules.entities.ProcessingUnit): Unit =
      cleanUpRan = true
    def demultiplex(unit: hercules.entities.ProcessingUnit)(implicit executor: ExecutionContext): Future[hercules.demultiplexing.DemultiplexingResult] = {
      // Block for the specified duration before returning the desired result
      println("*********** " + executor + " *************")
      def blockUntil(): Future[DemultiplexingResult] = {
        Thread.sleep(block.toMillis)
        Future(new DemultiplexingResult(unit, succeed, Some(logText)))(executor)
      }

      if (exception.isEmpty) blockUntil()
      else Future.failed(exception.get)
    }
  }

  val blockedDemuxActor = system.actorOf(
    SisyphusDemultiplexingExecutorActor.props(
      new FakeDemultiplexer(
        succeed = true,
        block = 20.seconds
      )),
    "SisyphusDemultiplexingExecutorActor_Blocked")

  // A "probe" actor with a custom dispatcher

  class CustomProbe extends Actor {
    def receive = LoggingReceive {
      case msg =>
        println("*** CustomProbe: " + msg + " ***")
    }
  }
  val customProbe = system.actorOf(Props(new CustomProbe), "BlockedActorProbe")

  override def afterAll(): Unit = {
    system.shutdown()
    Thread.sleep(1000)
  }

  "A SisyphusDemultiplexingExecutorActor" should " respond with Idle status when idle" in {

    blockedDemuxActor.tell(IlluminaDemultiplexingActorProtocol.RequestExecutorAvailabilityMessage, customProbe)

  }

  it should " become busy when a demultiplexing job is running" in {

    runfolder.mkdir()
    blockedDemuxActor.tell(HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit), customProbe)
    blockedDemuxActor.tell(IlluminaDemultiplexingActorProtocol.RequestExecutorAvailabilityMessage, customProbe)

  }

  it should " reject another demultiplex job when busy" in {

    blockedDemuxActor.tell(HerculesMainProtocol.StartDemultiplexingProcessingUnitMessage(processingUnit), customProbe)

  }

  it should " become idle after demultiplex job has finished" in {
    blockedDemuxActor.tell(HerculesMainProtocol.FinishedDemultiplexingProcessingUnitMessage(processingUnit), customProbe)
    blockedDemuxActor.tell(IlluminaDemultiplexingActorProtocol.RequestExecutorAvailabilityMessage, customProbe)
    blockedDemuxActor ! PoisonPill
    customProbe ! PoisonPill

  }

}