package hercules.actors.notifiers.rest.slack

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import hercules.config.notification.SlackNotificationConfig
import hercules.entities.notification.NotificationUnit
import hercules.protocols.HerculesMainProtocol.{ FailedNotificationUnitMessage, SentNotificationUnitMessage, SendNotificationUnitMessage }
import hercules.protocols.NotificationChannelProtocol
import org.scalatest.{ BeforeAndAfter, Matchers, FlatSpecLike }

import com.netaporter.precanned.dsl.fancy._
import spray.http.{ StatusCodes }

/**
 * Created by johda411 on 2015-03-18.
 */
class SlackNotifierExecutorTest extends TestKit(ActorSystem("SlackNotifierExecutorActorTestSystem"))
    with ImplicitSender
    with FlatSpecLike
    with BeforeAndAfter
    with Matchers {

  val mockBindingPort = 8766
  val mockSlack = httpServerMock(system).bind(mockBindingPort).block

  // Make sure expectations as cleared between tests.
  after { mockSlack.clearExpectations }

  val conf =
    new SlackNotificationConfig(
      channels = Seq(NotificationChannelProtocol.Progress),
      retryInterval = 1,
      numRetries = 1,
      slackEndPoint = s"http://127.0.0.1:${mockBindingPort}/slack",
      slackChannel = "awesome",
      slackUserName = "tester",
      iconEmoji = ":dancers:"
    )

  val unit = new NotificationUnit(message = "test message", channel = NotificationChannelProtocol.Progress)
  val progressMessage = new SendNotificationUnitMessage(unit)
  val actor = system.actorOf(SlackNotifierExecutor.props(conf))

  "A SlackNotifierExecutor" should "pass any message on to Slack" in {
    mockSlack expect post and path("/slack") and respond using status(StatusCodes.OK) end ()

    actor ! progressMessage
    expectMsg(SentNotificationUnitMessage(unit))

  }

  it should "be well behaved in failure" in {
    mockSlack expect post and path("/slack") and respond using status(StatusCodes.InternalServerError) end ()

    actor ! progressMessage
    expectMsg(FailedNotificationUnitMessage(unit, reason = "HTTP/1.1 500 Internal Server Error"))
  }
}
