package hercules.actors.notifiers.rest.ngipipeline

import java.io.File

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.netaporter.precanned.dsl.fancy._
import hercules.config.notification.NGIPipelineNotificationConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.entities.illumina.{ HiSeqProcessingUnit, IlluminaProcessingUnit }
import hercules.entities.notification.NotificationUnit
import hercules.protocols.HerculesMainProtocol.{ FailedNotificationUnitMessage, SentNotificationUnitMessage, FinishedDemultiplexingProcessingUnitMessage, SendNotificationUnitMessage }
import hercules.protocols.NotificationChannelProtocol
import org.scalatest.{ BeforeAndAfterAll, Matchers, BeforeAndAfter, FlatSpecLike }
import spray.http.StatusCodes

/**
 * Created by johda411 on 2015-03-20.
 */
class NGIPipelineNotifierExecutorTest extends TestKit(ActorSystem("SlackNotifierExecutorActorTestSystem"))
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfter
    with BeforeAndAfterAll
    with Matchers {

  val mockBindingPort = 8766
  val mockNgiPipeline = httpServerMock(system).bind(mockBindingPort).block

  // Make sure expectations as cleared between tests.
  after { mockNgiPipeline.clearExpectations }
  override def afterAll() { system.shutdown() }

  val conf = new NGIPipelineNotificationConfig(
    channels = Seq(NotificationChannelProtocol.Progress),
    retryInterval = 1,
    numRetries = 3,
    host = "localhost",
    port = mockBindingPort)

  val runfolder = new File("runfolder1")
  val processingUnit: IlluminaProcessingUnit =
    new HiSeqProcessingUnit(
      new IlluminaProcessingUnitConfig(
        new File("Samplesheet1"),
        new File("DefaultQC"),
        Some(new File("DefaultProg"))),
      runfolder.toURI())

  val finishedDemultiplexingProcessingUnitMessage = FinishedDemultiplexingProcessingUnitMessage(unit = processingUnit)

  val unit =
    new NotificationUnit(
      message = "test message",
      channel = NotificationChannelProtocol.Progress,
      originalMessage = Some(finishedDemultiplexingProcessingUnitMessage))
  val progressMessage = new SendNotificationUnitMessage(unit)

  val actor = system.actorOf(NGIPipelineNotifierExecutor.props(conf))

  "A NGIPipelineNotifierExecutor" should "trigger the NGI pipline when there is a finished demultiplexing message." in {

    actor ! progressMessage
    mockNgiPipeline expect path(s"/flowcell_analysis/${runfolder.getName()}") and respond using status(StatusCodes.OK) end ()
    expectMsg(SentNotificationUnitMessage(unit))

  }

  it should "throw and exception and exit gracefully if it's sent a message without info on the original message" in {

    val unit =
      new NotificationUnit(
        message = "test message",
        channel = NotificationChannelProtocol.Progress)
    val progressMessage = new SendNotificationUnitMessage(unit)
    actor ! progressMessage
    // Don't care about the reason in the message for now...
    expectMsgType[FailedNotificationUnitMessage]
  }

}
