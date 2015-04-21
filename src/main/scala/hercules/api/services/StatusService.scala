package hercules.api.services

import akka.actor.ActorRef
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern.ask
import akka.util.Timeout

import com.wordnik.swagger.annotations._
import hercules.actors.masters.state.MasterState

import scala.concurrent.ExecutionContext

import spray.http.StatusCodes.InternalServerError
import hercules.protocols.HerculesMainProtocol.RequestMasterState
import hercules.api.models

import spray.httpx.SprayJsonSupport._

import scala.concurrent.duration._
import scala.util.{ Success, Failure }

/**
 * The StatusService trait define operations for querying the status of the tasks in Master.
 */
@Api(
  value = "/status",
  description = "Obtain the current status of Hercules tasks.")
trait StatusService extends HerculesService {

  implicit def ec: ExecutionContext = actorRefFactory.dispatcher
  implicit val clusterClient: ActorRef
  implicit val to: Timeout

  @ApiOperation(
    value = "Get the status of jobs sent to Hercules",
    notes = "Returns a MasterState object",
    httpMethod = "GET",
    response = classOf[models.MasterState],
    nickname = "Status",
    produces = "application/json")
  @ApiImplicitParams(Array())
  @ApiResponses(
    Array(
      new ApiResponse(
        code = 500,
        message = "An exception occurred"),
      new ApiResponse(
        code = 200,
        message = "OK")
    )) /**
   * The concatenated route for this service
   */
  def route = getRoute

  /**
   * The /status endpoint for GET
   */
  private def getRoute =
    get {
      path("status") {
        /**
         * Complete the request asynchronously
         */
        detach() {

          val timeout = Timeout(3.seconds)
          /**
           * Request the state of the active master
           */
          val request =
            clusterClient.ask(
              SendToAll(
                "/user/master/active",
                RequestMasterState())
            )(timeout)

          /**
           * Take the response from master and map it to a StatusCode
           */

          import hercules.actors.masters.state.MasterStateJsonProtocol._

          onComplete(request) {
            case Success(state) => complete(state.asInstanceOf[MasterState])
            case Failure(_)     => complete(InternalServerError)
          }

        }
      }
    }
}
