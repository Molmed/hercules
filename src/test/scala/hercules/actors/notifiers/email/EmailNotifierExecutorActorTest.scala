package hercules.actors.notifiers.email

import akka.actor.{ Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import hercules.actors.notifiers.NotificationExecutor
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit
import hercules.protocols.HerculesMainProtocol._
import hercules.protocols.NotificationChannelProtocol._
import org.jvnet.mock_javamail.Mailbox
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }

/**
 * This test class uses Mock JavaMail to test that messages
 * are sent (see: https://github.com/softprops/courier for details).
 */
class EmailNotifierExecutorActorTest()
    extends TestKit(ActorSystem("EmailNotifierExecutorActorTestSystem"))
    with ImplicitSender
    with FlatSpecLike
    with Matchers {

  import scala.concurrent.duration._

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

  "An EmailNotifierExecutorActor" should "send emails" in {

    val actor = TestActorRef(
      EmailNotifierExecutorActorImpl.props(conf),
      "EmailNotifierExecutorActorTestActorRef")

    val unit = new FakeEmailNotificationObjects.FakeEmailNotificationUnit(
      "This is a FakeEmailNotificationUnit",
      Info,
      0,
      None)
    actor ! SendNotificationUnitMessage(unit)
    expectMsg(3.seconds, SentNotificationUnitMessage(unit))

    // Check that there was actually an email sent.
    val inbox = Mailbox.get("test@test.com")
    assert(inbox.size() === 1)
    assert(inbox.get(0).getContent() === "This is a FakeEmailNotificationUnit")
    inbox.clear()
  }

  it should " return a FailedNotificationUnitMessage to parent upon failure to send message" in {

    val testException = new Exception("This is a test exception!")

    class EmailNotifierExecutorActorThatGoesBoom extends EmailNotifierExecutorActor with NotificationExecutor with Emailer {
      override def emailConfig: EmailNotificationConfig = conf
      override def sendEmail(email: EmailNotificationUnit, emailConfig: EmailNotificationConfig): Unit = {
        val parentActor = sender
        parentActor ! FailedNotificationUnitMessage(email.copy(attempts = email.attempts + 1), testException.getMessage)
      }
    }

    val actor = TestActorRef(
      Props(new EmailNotifierExecutorActorThatGoesBoom()),
      "EmailNotifierExecutorActorWithExceptionTestActorRef")

    val attempts: Int = 0
    val unit = new FakeEmailNotificationObjects.FakeEmailNotificationUnit(
      "This is a FakeEmailNotificationUnit",
      Info,
      attempts,
      Some(testException))
    actor ! SendNotificationUnitMessage(unit)
    expectMsg(3.seconds, FailedNotificationUnitMessage(unit.copy(attempts = (attempts + 1)), testException.getMessage))
  }
}