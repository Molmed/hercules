package hercules.actors.notifiers.rest.ngipipeline

import java.io.File

import akka.actor.ActorSystem
import akka.testkit.TestKit
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.entities.illumina.{ HiSeqProcessingUnit, IlluminaProcessingUnit }
import hercules.entities.notification.NotificationUnit
import hercules.protocols.HerculesMainProtocol.{ StartDemultiplexingProcessingUnitMessage, FinishedDemultiplexingProcessingUnitMessage, SendNotificationUnitMessage }
import hercules.protocols.NotificationChannelProtocol
import org.scalatest.{ FlatSpecLike, FunSuite }

/**
 * Tests for the NGIPiperlineNotifierActor
 * Created by johda411 on 2015-03-20.
 */
class NGIPipelineNotifierActorTest extends TestKit(ActorSystem("NGIPipelineNotifierActorTestSystem"))
    with FlatSpecLike {

  "A NGIPipelineNotifierActor" should "start up without throwing exceptions" in {
    val actor = NGIPipelineNotifierActor(system)
  }

  it should "pre-filter keep if original message was a FinishedDemultiplexingProcessingUnitMessage and filter out other messages" in {

    val runfolder = new File("runfolder1")
    val processingUnit: IlluminaProcessingUnit =
      new HiSeqProcessingUnit(
        new IlluminaProcessingUnitConfig(
          new File("Samplesheet1"),
          new File("DefaultQC"),
          Some(new File("DefaultProg"))),
        runfolder.toURI())

    val finishedDemultiplexingProcessingUnitMessage = FinishedDemultiplexingProcessingUnitMessage(unit = processingUnit)
    val startDemultiplexingProcessingUnitMessage = StartDemultiplexingProcessingUnitMessage(unit = processingUnit)

    val keepThis =
      SendNotificationUnitMessage(
        new NotificationUnit(
          message = "test",
          originalMessage = Some(finishedDemultiplexingProcessingUnitMessage),
          channel = NotificationChannelProtocol.Critical))

    val filterThis = SendNotificationUnitMessage(
      new NotificationUnit(
        message = "test",
        originalMessage = Some(startDemultiplexingProcessingUnitMessage),
        channel = NotificationChannelProtocol.Critical))

    val alsoFilterIfThereIsNoOriginalMessage = SendNotificationUnitMessage(
      new NotificationUnit(
        message = "test",
        channel = NotificationChannelProtocol.Critical))

    assert(NGIPipelineNotifierActor.notifierActorChannelPreFilter(keepThis) === true)
    assert(NGIPipelineNotifierActor.notifierActorChannelPreFilter(filterThis) === false)
    assert(NGIPipelineNotifierActor.notifierActorChannelPreFilter(alsoFilterIfThereIsNoOriginalMessage) === false)
  }

}
