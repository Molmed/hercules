package hercules.api.services

import akka.actor.{ ActorRef, ActorRefFactory }
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern.ask
import akka.util.Timeout

import com.wordnik.swagger.annotations._

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization

import scala.concurrent.{ Await, duration, ExecutionContext }
import scala.util.{ Failure, Success }

import spray.http.StatusCodes._
import spray.routing._

import hercules.actors.masters.{ MasterState, MasterStateProtocol }
import hercules.entities.ProcessingUnit
import hercules.protocols.HerculesMainProtocol._
import hercules.api.{ BootedCore, CoreActors }

@Api(
  value = "/status",
  description = "Obtain the current status of Hercules tasks.")
trait StatusService extends HttpService {
  this: BootedCore with CoreActors =>

  import duration._
  implicit def ec: ExecutionContext = system.dispatcher

  def route = getRoute

  @ApiOperation(
    value = "Get the status of jobs sent to Hercules",
    notes = "Returns a MasterState object",
    httpMethod = "GET",
    response = classOf[MasterState])
  @ApiResponses(
    Array(
      new ApiResponse(
        code = 500,
        message = "An exception occurred"),
      new ApiResponse(
        code = 200,
        message = "OK",
        response = classOf[MasterState])
    ))
  private def getRoute = get {
    path("status") {
      detach() {
        complete {
          val request =
            cluster.ask(
              SendToAll(
                "/user/master/active",
                RequestMasterState())
            )
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
          }.recover {
            case e: Exception =>
              InternalServerError
          }
          response
        }
      }
    }
  }
  implicit val formats = Serialization.formats(NoTypeHints)
}
