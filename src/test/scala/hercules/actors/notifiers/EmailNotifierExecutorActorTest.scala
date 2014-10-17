package hercules.actors.notifiers

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActor, TestActorRef, TestKit }

import com.typesafe.config.Config

import hercules.actors.notifiers._
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.{ NotificationUnit, EmailNotificationUnit }
import hercules.protocols.HerculesMainProtocol._
import hercules.protocols.NotificationChannelProtocol._

import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }

import scala.concurrent.{ duration, Future }

class EmailNotifierExecutorActorTest()
    extends TestKit(ActorSystem("EmailNotifierExecutorActorTestSystem"))
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfterAll
    with Matchers {

  import duration._

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val conf = new EmailNotificationConfig(
    recipients = Seq("test@test.com"),
    sender = "EmailNotifierExecutorActorTest",
    smtpHost = "localhost",
    smtpPort = 25,
    prefix = "[HerculesTestSuite]",
    numRetries = 1,
    retryInterval = 5,
    channels = Seq()
  )

  val actor = TestActorRef(
    EmailNotifierExecutorActor.props(conf),
    "EmailNotifierExecutorActorTestActorRef")

  "An EmailNotifierExecutorActor" should " return a failure message if not sent an EmailNotificationUnit " in {
    val unit = new NotificationUnit(
      "This is a NotificationUnit",
      Info)
    actor ! SendNotificationUnitMessage(unit)
    expectMsg(3.seconds, FailedNotificationUnitMessage(unit, "Unhandled unit type: " + unit.getClass.getSimpleName))
  }

  it should " return a SentNotificationMessage to parent upon successful sending of message" in {
    val unit = new FakeEmailNotificationObjects.FakeEmailNotificationUnit(
      "This is a FakeEmailNotificationUnit",
      Info,
      0,
      None)
    actor ! SendNotificationUnitMessage(unit)
    expectMsg(3.seconds, SentNotificationUnitMessage(unit))
  }

  it should " return a FailedNotificationUnitMessage to parent upon failure to send message" in {
    val e = new Exception("This is a test exception")
    val attempts: Int = 0
    val unit = new FakeEmailNotificationObjects.FakeEmailNotificationUnit(
      "This is a FakeEmailNotificationUnit",
      Info,
      attempts,
      Some(e))
    actor ! SendNotificationUnitMessage(unit)
    expectMsg(3.seconds, FailedNotificationUnitMessage(unit.copy(attempts = (attempts + 1)), e.getMessage))
  }
}