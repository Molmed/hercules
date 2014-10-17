package hercules.actors.notifiers

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActor, TestActorRef, TestKit, TestProbe }

import com.typesafe.config.Config

import hercules.actors.notifiers._
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.{ NotificationUnit, EmailNotificationUnit }
import hercules.protocols.HerculesMainProtocol._
import hercules.protocols.NotificationChannelProtocol._

import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }

import scala.concurrent.{ duration, Future }

class EmailNotifierActorTest()
    extends TestKit(ActorSystem("EmailNotifierActorTestSystem"))
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
    sender = "EmailNotifierActorTest",
    smtpHost = "localhost",
    smtpPort = 25,
    prefix = "[HerculesTestSuite]",
    numRetries = 1,
    retryInterval = 2,
    channels = Seq(Info)
  )

  val executor = new TestProbe(system)
  val actor = TestActorRef(
    EmailNotifierActor.props(conf, executor.testActor),
    "EmailNotifierActorTestActorRef")

  "An EmailNotifierActor" should "wrap a NotificationUnit as an EmailNotificationUnit and send to executor (via self)" in {
    val unit = new NotificationUnit(
      "This is a NotificationUnit",
      Info)
    actor ! SendNotificationUnitMessage(unit)
    executor.expectMsg(3.seconds, SendNotificationUnitMessage(EmailNotificationUnit.wrapNotificationUnit(unit)))
  }

  it should "pass a SendNotificationMessage containing an EmailNotificationUnit to executor if it is sent in a channel it listens to" in {
    val unit = new EmailNotificationUnit(
      "This is an EmailNotificationUnit",
      Info,
      0)
    actor ! SendNotificationUnitMessage(unit)
    executor.expectMsg(3.seconds, SendNotificationUnitMessage(unit))
  }

  it should "not pass a SendNotificationMessage containing an EmailNotificationUnit to executor if it is sent in a channel it ignores" in {
    val unit = new EmailNotificationUnit(
      "This is an EmailNotificationUnit",
      Critical,
      0)
    actor ! SendNotificationUnitMessage(unit)
    executor.expectNoMsg(5.seconds)
  }

  it should "retry a SendNotificationMessage after the specified interval if the maximum number of retries have not been met" in {
    val unit = new EmailNotificationUnit(
      "This EmailNotificationUnit should be retried once",
      Info,
      0)
    actor ! FailedNotificationUnitMessage(unit, "Testing failure")
    executor.expectMsg((2 * conf.retryInterval).seconds, SendNotificationUnitMessage(unit))

  }

  it should "not retry a SendNotificationMessage if the maximum number of retries have been met" in {
    val unit = new EmailNotificationUnit(
      "This EmailNotificationUnit should not be retried",
      Info,
      2)
    actor ! FailedNotificationUnitMessage(unit, "Testing failure")
    executor.expectNoMsg((2 * conf.retryInterval).seconds)
  }
}