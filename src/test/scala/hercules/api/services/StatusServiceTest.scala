package hercules.api.services

import akka.contrib.pattern.ClusterClient.SendToAll
import akka.util.Timeout
import hercules.actors.masters.MasterStateProtocol.RemoveFromFailedMessages
import hercules.actors.masters.state.MasterState
import hercules.protocols.HerculesMainProtocol._
import hercules.test.utils.ProcessingUnitPlaceholder
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._
import scala.util.parsing.json.JSONObject

class StatusServiceTest
    extends FlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalatestRouteTest {

  val messagesNotYetProcessed = Set("this-unit-is-not-yet-processed")
  val messagesInProcessing = Set("this-unit-is-being-processed")
  val failedMessages = Set("this-unit-failed-in-processing")

  val masterState = MasterState(
    messagesNotYetProcessed.map { (id: String) => FoundProcessingUnitMessage(ProcessingUnitPlaceholder(id)) },
    messagesInProcessing.map { (id: String) => StartDemultiplexingProcessingUnitMessage(ProcessingUnitPlaceholder(id)) },
    failedMessages.map { (id: String) => FailedDemultiplexingProcessingUnitMessage(ProcessingUnitPlaceholder(id), "Testing failure") }
  )

  val probe = MockBackend(
    system = this.system,
    masterState,
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

  "A GET request to /status" should "return correct json if everything is empty" in {
    Get("/status") ~> service.route ~> check {
      entity.data.asString should be(masterState.toJson.prettyPrint)
    }
  }

  "A GET request to /status" should "return the updated state if something happend in the backend." in {
    // Remove the failed message
    for (mess <- masterState.failedMessages)
      probe.ref ! RemoveFromFailedMessages(Some(mess))

    val updateMasterState = masterState.copy(failedMessages = Set())

    // Ensure that the state has update to match.
    Get("/status") ~> service.route ~> check {
      entity.data.asString should be(updateMasterState.toJson.prettyPrint)
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
      masterState,
      doNotAnswer = true)

    val exceptionService = new StatusService {
      def actorRefFactory = system
      implicit val to = timeout
      implicit val clusterClient = exceptionProbe.ref
    }

    Get("/status") ~> exceptionService.route ~> check {
      status should be(InternalServerError)
    }
  }
}
