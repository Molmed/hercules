package hercules.actors.notifiers

import akka.actor.{ Props, ActorRef, ActorSystem }
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import hercules.config.notification.NotificationConfig
import hercules.entities.notification.NotificationUnit
import hercules.protocols.HerculesMainProtocol.{ FailedNotificationUnitMessage, SendNotificationUnitMessage }
import hercules.protocols.NotificationChannelProtocol
import hercules.protocols.NotificationChannelProtocol._
import org.scalatest.{ FlatSpecLike, Matchers }

/**
 * Tests the notifer base actor, making sure that any forwarding behaviour works as
 * expected.
 * Created by johda411 on 2015-03-17.
 */
class NotifierActorTest extends TestKit(ActorSystem("NotifierActor"))
    with FlatSpecLike
    with Matchers {

  val testProbe = TestProbe()

  val notifierActor = system.actorOf(
    Props(
      new NotifierActor {
        override def executor: ActorRef = testProbe.ref
        override def notifierConfig: NotificationConfig = new NotificationConfig {
          override val retryInterval: Int = 1
          override val channels: Seq[NotificationChannel] =
            Seq(NotificationChannelProtocol.Warning, NotificationChannelProtocol.Critical)
          override val numRetries: Int = 2
        }
      }))

  "A NotifierActor" should " pass messages on to it's executor if it's sent on a channel to which is is " +
    "subscribing" in {

      val warningMessage = new NotificationUnit(message = "test message", channel = NotificationChannelProtocol.Warning)
      val expectedMessage = SendNotificationUnitMessage(warningMessage)
      notifierActor ! expectedMessage
      testProbe.expectMsg(expectedMessage)
    }

  it should "drop messages on channels to which it's not subscribing" in {

    val infoMessage = new NotificationUnit(message = "test message", channel = NotificationChannelProtocol.Info)
    val expectedMessage = SendNotificationUnitMessage(infoMessage)
    notifierActor ! expectedMessage
    testProbe.expectNoMsg()
  }

  it should "resend messages up to a maximum number of retries" in {
    val notificationUnitToRetry = new NotificationUnit(message = "test message", channel = NotificationChannelProtocol.Warning, attempts = 1)

    val failedMessageShouldBeResent =
      FailedNotificationUnitMessage(
        notificationUnitToRetry,
        reason = "A reason as good as any...")

    val failedMessageShouldNotBeResent = FailedNotificationUnitMessage(
      new NotificationUnit(message = "test message", channel = NotificationChannelProtocol.Warning, attempts = 3),
      reason = "To many drinks last night...")

    notifierActor ! failedMessageShouldBeResent
    testProbe.expectMsg(SendNotificationUnitMessage(notificationUnitToRetry))

    notifierActor ! failedMessageShouldNotBeResent
    testProbe.expectNoMsg()
  }

  it should "filter out messages acccoding to what is defined by it's preFilterChannel function" in {

    val keepString = "Keep me!"
    val messageToDrop = SendNotificationUnitMessage(new NotificationUnit(message = "test message", channel = NotificationChannelProtocol.Warning))
    val messageToKeep = SendNotificationUnitMessage(new NotificationUnit(message = keepString, channel = NotificationChannelProtocol.Warning))

    val notifierActorWithFilter = system.actorOf(
      Props(
        new NotifierActor {
          override def executor: ActorRef = testProbe.ref

          override def notifierConfig: NotificationConfig = new NotificationConfig {
            override val retryInterval: Int = 1
            override val channels: Seq[NotificationChannel] =
              Seq(NotificationChannelProtocol.Warning, NotificationChannelProtocol.Critical)
            override val numRetries: Int = 2
          }

          override def preFilterChannel(notificationMessage: SendNotificationUnitMessage): Boolean = {
            notificationMessage.unit.message == keepString
          }
        }))

    notifierActorWithFilter ! messageToDrop
    testProbe.expectNoMsg()
    notifierActorWithFilter ! messageToKeep
    testProbe.expectMsg(messageToKeep)

  }
}
