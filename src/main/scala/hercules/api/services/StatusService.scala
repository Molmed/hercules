package hercules.api.services

import akka.actor.ActorRef
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern.ask
import akka.util.Timeout

import com.wordnik.swagger.annotations._

import scala.concurrent.ExecutionContext

import spray.http.StatusCodes._
import spray.routing._

import hercules.actors.masters.MasterState
import hercules.protocols.HerculesMainProtocol.RequestMasterState
import hercules.api.models

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
  private def getRoute = get {
    path("status") {
      /**
       * Complete the request asynchronously
       */
      detach() {
        complete {
          /**
           * Request the state of the active master
           */
          val request =
            clusterClient.ask(
              SendToAll(
                "/user/master/active",
                RequestMasterState())
            )
          /**
           * Take the response from master and map it to a StatusCode
           */
          val response = request.map {
            case MasterState(messagesNotYetProcessed, messagesInProcessing, failedMessages) => {
              // @TODO Let some json marshaller handle the response instead
              val status =
                "messagesNotYetProcessed: {" +
                  messagesNotYetProcessed.mkString(",") +
                  "}, messagesInProcessing: {" +
                  messagesInProcessing.mkString(",") +
                  "} ,failedMessages: {" +
                  failedMessages.mkString("}")
              OK
            }
            /**
             * Handle exceptions that may be thrown
             */
          }.recover {
            case e: Exception =>
              InternalServerError
          }
          response
        }
      }
    }
  }
}
