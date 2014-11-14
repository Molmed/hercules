package hercules.api.services

import akka.actor.{ Actor, ActorRef }
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern.ask
import akka.util.Timeout

import com.wordnik.swagger.annotations._

import hercules.protocols.HerculesMainProtocol._
import hercules.actors.masters.{ MasterState, MasterStateProtocol }

import javax.ws.rs.Path

import scala.concurrent.ExecutionContext

import spray.http.StatusCodes._
import spray.routing._

/**
 * The DemultiplexingService trait define operations for interacting with tasks related to demultiplexing.
 */
@Api(
  value = "/demultiplex",
  description = "Control demultiplexing tasks.")
trait DemultiplexingService extends HerculesService {

  implicit def ec: ExecutionContext = actorRefFactory.dispatcher
  implicit val clusterClient: ActorRef
  implicit val to: Timeout
  import MasterStateProtocol._

  /**
   * The concatenated route for this service
   */
  def route =
    pathPrefix("demultiplex" / Segment) { id =>
      restartFailedDemultiplexJob(id) ~
        removeFailedDemultiplexJob(id) ~
        forgetDemultiplexJob(id) ~
        stopRunningDemultiplexJob(id)
    }

  /**
   * Tell the master to restart a demultiplex job that has previously
   * failed and which the master has in its list of failed units
   */
  @Path("/{ID}/restart")
  @ApiOperation(
    value = "Restart a failed demultiplex task",
    notes = "Send a message requesting that the Master will re-queue a previously failed demultiplex task.",
    httpMethod = "PUT",
    nickname = "Restart",
    produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "ID", value = "ID of the Processing Unit to process. Typically corresponds to the name of the runfolder.", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(
    Array(
      new ApiResponse(
        code = 500,
        message = "An exception occurred"),
      new ApiResponse(
        code = 202,
        message = "Accepted")
    ))
  def restartFailedDemultiplexJob(id: String) =
    path("restart") {
      put {
        /**
         * Complete the request asynchronously
         */
        detach() {
          complete {
            /**
             * Send a request to restart the Processing Unit corresponding to the supplied id. The response is mapped to an appropriate StatusCode.
             */
            clusterClient.ask(
              SendToAll(
                "/user/master/active",
                RestartDemultiplexingProcessingUnitMessage(id))
            ).map {
                case Acknowledge =>
                  Accepted
                case Reject(reason) =>
                  NotFound
                /**
                 * Handle exceptions that may be thrown.
                 */
              }.recover {
                case e: Exception =>
                  InternalServerError
              }
          }
        }
      }
    }

  /**
   * Remove a failed demultiplex job from the master's list of failed demultiplex jobs.
   * If successful, this job will not be retried.
   */
  @Path("/{ID}/remove")
  @ApiOperation(
    value = "Remove a failed demultiplex task",
    notes = "Send a message requesting that the Master will remove a previously failed demultiplex task from its list of tasks.",
    httpMethod = "DELETE",
    nickname = "Remove",
    produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "ID", value = "ID of the Processing Unit to process. Typically corresponds to the name of the runfolder.", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(
    Array(
      new ApiResponse(
        code = 500,
        message = "An exception occurred"),
      new ApiResponse(
        code = 200,
        message = "OK"),
      new ApiResponse(
        code = 404,
        message = "Not found")
    ))
  def removeFailedDemultiplexJob(id: String) =
    path("remove") {
      delete {
        /**
         * Complete the request asynchronously
         */
        detach() {
          complete {
            /**
             * Request the message containing the Processing Unit from master.
             */
            val request =
              clusterClient.ask(
                SendToAll(
                  "/user/master/active",
                  RequestMasterState(Some(id)))
              )
            /**
             * If a matching message was found, request master to remove it from its state, otherwise return a 404 status code.
             */
            val response = request.map {
              case ms: MasterState => {
                val matchingMessage = ms.findStateOfUnit(Some(id)).failedMessages.headOption
                if (!matchingMessage.isEmpty) {
                  clusterClient.tell(SendToAll("/user/master/active", RemoveFromFailedMessages(matchingMessage)), Actor.noSender)
                  OK
                } else {
                  NotFound
                }
              }
              /**
               * If we did not get a MasterState back, something is not right
               */
              case _ =>
                InternalServerError
            }.recover {
              case e: Exception =>
                InternalServerError
            }
            response
          }
        }
      }
    }

  /**
   * Forget any previous demultiplex results for the specified unit.
   * This should make it discoverable again by the
   * ProcessUnitWatcher and trigger a new demultiplexing job
   */
  @Path("/{ID}/forget")
  @ApiOperation(
    value = "Forget that a Processing Unit has been demultiplexed",
    notes = "Send a message requesting that the Master will make sure that any previous demultiplexing results for a Processing Unit are forgotten. The Processing Unit will then be available for being picked up and re-demultiplexed.",
    httpMethod = "DELETE",
    nickname = "Forget",
    produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "ID", value = "ID of the Processing Unit to process. Typically corresponds to the name of the runfolder.", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(
    Array(
      new ApiResponse(
        code = 500,
        message = "An exception occurred"),
      new ApiResponse(
        code = 200,
        message = "OK"),
      new ApiResponse(
        code = 404,
        message = "Not found")
    ))
  def forgetDemultiplexJob(id: String) =
    path("forget") {
      delete {
        /**
         * Complete the request asynchronously
         */
        detach() {
          complete {
            /**
             * Request that master takes care of removing the traces of a previous demultiplexing of the corresponding Processing Unit.
             */
            clusterClient.tell(
              SendToAll("/user/master/active",
                ForgetDemultiplexingProcessingUnitMessage(id)),
              Actor.noSender)
            // @TODO Oops.. handle the response, this has been implemented
            NotImplemented
          }
        }
      }
    }

  /**
   * Stop an ongoing demultiplex task on the specified unit
   */
  @Path("/{ID}/stop")
  @ApiOperation(
    value = "Stop an ongoing demultiplex task on a Processing Unit",
    notes = "Send a message requesting that the Master will request an ongoing demultiplex task to be stopped.",
    httpMethod = "PUT",
    nickname = "Stop",
    produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "ID", value = "ID of the Processing Unit to process. Typically corresponds to the name of the runfolder.", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(
    Array(
      new ApiResponse(
        code = 501,
        message = "Not implemented")
    ))
  def stopRunningDemultiplexJob(id: String) =
    path("stop") {
      put {
        detach() {
          /**
           * Complete the request asynchronously
           */
          complete {
            /**
             * Request that master takes care of stopping ongoing demultiplex tasks for the specified Processing Unit
             */
            clusterClient.tell(
              SendToAll("/user/master/active", StopDemultiplexingProcessingUnitMessage(id)),
              Actor.noSender)
            NotImplemented
          }
        }
      }
    }

}
