package hercules.actors.notifiers.email

import akka.actor.{ Props, Actor, ActorLogging, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit }
import hercules.config.notification.EmailNotificationConfig
import hercules.entities.notification.EmailNotificationUnit
import hercules.protocols.HerculesMainProtocol.{ FailedNotificationUnitMessage, SentNotificationUnitMessage }
import hercules.protocols.NotificationChannelProtocol.{ NotificationChannel, Info }
import org.jvnet.mock_javamail.Mailbox
import org.scalatest.{ Matchers, BeforeAndAfterAll, FlatSpecLike }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Tests the Emailer trait, a trait that can be hooked on to an actor to enable it to send emails.
 *
 * Created by johda411 on 2015-03-17.
 */
class EmailerTest extends TestKit(ActorSystem("EmailNotifierExecutorActorTestSystem"))
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfterAll
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

  class EmailerActor() extends Emailer with Actor with ActorLogging {
    override def receive: Receive = {
      case email: EmailNotificationUnit =>
        sendEmail(email, conf)
    }
  }

  val emailer = system.actorOf(Props(new EmailerActor()))

  "A Emailer " should " send emails " in {

    val email = new EmailNotificationUnit(message = "test", channel = Info)
    emailer ! email

    expectMsg(3.seconds, SentNotificationUnitMessage(email))
    val inbox = Mailbox.get("test@test.com")
    assert(inbox.size() === 1)
    assert(inbox.get(0).getContent() === "test")
    inbox.clear()

  }

  it should "recover from failing to send by reporting it to the parent" in {

    import ExecutionContext.Implicits.global

    val exceptionString = "This is not a good day for email!"

    class EmailNotificationUnitWithException(override val message: String,
                                             override val channel: NotificationChannel,
                                             override val attempts: Int = 0) extends EmailNotificationUnit(message, channel, attempts) {

      override def sendNotification(recipients: Seq[String], sender: String, prefix: String, smtpHost: String, smtpPort: Int): Future[Unit] =
        Future { throw new Exception(exceptionString) }
    }

    val exceptionCausingEmail = new EmailNotificationUnitWithException("test exception", channel = Info)
    emailer ! exceptionCausingEmail

    expectMsg(3.seconds, FailedNotificationUnitMessage(exceptionCausingEmail.copy(attempts = exceptionCausingEmail.attempts + 1), reason = exceptionString))

    val inbox = Mailbox.get("test@test.com")
    assert(inbox.size() === 0)
    inbox.clear()
  }

}
