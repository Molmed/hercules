package hercules.api.services

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.testkit.{ TestKit, TestProbe }
import akka.util.Timeout

import hercules.actors.masters.MasterStateProtocol
import hercules.protocols.HerculesMainProtocol._

import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import spray.testkit.ScalatestRouteTest
import spray.http.StatusCodes._
import spray.routing.Directives

class StatusServiceTest
    extends FlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalatestRouteTest {

  val probe = MockBackend(
    system = this.system,
    messagesNotYetProcessed = Set("this-unit-is-not-yet-processed"),
    messagesInProcessing = Set("this-unit-is-being-processed"),
    failedMessages = Set("this-unit-failed-in-processing"),
    doNotAnswer = false)

  val timeout = Timeout(5.seconds)
  val service = new StatusService {
    def actorRefFactory = system
    implicit val to = timeout
    implicit val clusterClient = probe.ref
  }

  override def afterAll(): Unit = {
    system.shutdown()
    Thread.sleep(1000)
  }

  "A GET request to /status" should " return a OK status code" in {
    Get("/status") ~> service.route ~> check {
      status should be(OK)
    }
  }
  it should "trigger a RequestMasterState to master" in {
    probe.expectMsg(3.seconds,
      SendToAll(
        "/user/master/active",
        RequestMasterState(None)))
  }

  it should "fail gracefully if an exception is thrown" in {

    // Ensure that this timeout is longer than the one used by
    // the actual service.
    implicit val routeTestTimeout = RouteTestTimeout(7.second)

    val exceptionProbe = MockBackend(
      system = this.system,
      messagesNotYetProcessed = Set("this-unit-is-not-yet-processed"),
      messagesInProcessing = Set("this-unit-is-being-processed"),
      failedMessages = Set("this-unit-failed-in-processing"),
      doNotAnswer = true)

    val exceptionService = new StatusService {
      def actorRefFactory = system
      implicit val to = timeout
      implicit val clusterClient = exceptionProbe.ref
    }

    Get("/status") ~> exceptionService.route ~> check {
      println(body)
      status should be(InternalServerError)
    }
  }
}
