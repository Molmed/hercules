package hercules.actors.notifiers.rest

import akka.actor.{ ActorLogging, Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit }
import hercules.entities.notification.NotificationUnit
import hercules.protocols.HerculesMainProtocol.{ FailedNotificationUnitMessage, SentNotificationUnitMessage, SendNotificationUnitMessage, NotificationUnitMessage }
import hercules.protocols.NotificationChannelProtocol
import org.scalatest.{ Matchers, FlatSpecLike, FunSuite }

import scala.concurrent.Future
import scalaj.http.HttpResponse

/**
 * Created by johda411 on 2015-03-18.
 */
class RestNotifierExecutorTest extends TestKit(ActorSystem("EmailNotifierExecutorActorTestSystem"))
    with ImplicitSender
    with FlatSpecLike
    with Matchers {

  class WellBehavedRestNotifierExecutor() extends RestNotifierExecutor with Actor with ActorLogging with RestClient {
    override def mapMessageToEndPoint(message: NotificationUnitMessage): Future[HttpResponse[String]] = {
      import context.dispatcher
      Future { new HttpResponse(body = "", code = 200, headers = Map()) }
    }
  }

  class WithNiceErrorRestNotifierExecutor() extends RestNotifierExecutor with Actor with ActorLogging with RestClient {
    override def mapMessageToEndPoint(message: NotificationUnitMessage): Future[HttpResponse[String]] = {
      import context.dispatcher
      Future { new HttpResponse(body = "", code = 500, headers = Map("Status" -> "500")) }
    }
  }

  class WithUglyErrorRestNotifierExecutor() extends RestNotifierExecutor with Actor with ActorLogging with RestClient {
    override def mapMessageToEndPoint(message: NotificationUnitMessage): Future[HttpResponse[String]] = {
      import context.dispatcher
      Future { throw new Exception("A really ugly error!") }
    }
  }

  val unit = new NotificationUnit(message = "test message", channel = NotificationChannelProtocol.Progress)
  val progressMessage = new SendNotificationUnitMessage(unit)

  "A RestNotifierExecutor" should "map incoming SendNotificationUnitMessage and respond correctly to success" in {
    val wellBehavedActor = system.actorOf(Props(new WellBehavedRestNotifierExecutor()))
    wellBehavedActor ! progressMessage
    expectMsg(SentNotificationUnitMessage(unit))
  }

  it should "handle pretty failure (e.g. the server not responding)" in {
    val withNiceErrorActor = system.actorOf(Props(new WithNiceErrorRestNotifierExecutor()))
    withNiceErrorActor ! progressMessage
    expectMsg(FailedNotificationUnitMessage(unit, reason = "500"))
  }

  it should "handle ugly failures (like an exception being thrown" in {
    val withUglyErrorActor = system.actorOf(Props(new WithUglyErrorRestNotifierExecutor()))
    withUglyErrorActor ! progressMessage
    expectMsg(FailedNotificationUnitMessage(unit, reason = "A really ugly error!"))
  }

}
