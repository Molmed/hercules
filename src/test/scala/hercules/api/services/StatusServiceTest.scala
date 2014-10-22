package hercules.api.services

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.testkit.{ TestKit, TestProbe }

import hercules.actors.masters.MasterStateProtocol
import hercules.api.services.StatusService
import hercules.protocols.HerculesMainProtocol._

import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }

import scala.concurrent.duration._

import spray.testkit.ScalatestRouteTest
import spray.http.StatusCodes._
import spray.routing.Directives

class StatusServiceTest
    extends FlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalatestRouteTest {

  import MasterStateProtocol._

  val probe = MockBackend(
    system = this.system,
    messagesNotYetProcessed = Set("this-unit-is-not-yet-processed"),
    messagesInProcessing = Set("this-unit-is-being-processed"),
    failedMessages = Set("this-unit-failed-in-processing"))
  val service = new StatusService(probe.ref)

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
}
