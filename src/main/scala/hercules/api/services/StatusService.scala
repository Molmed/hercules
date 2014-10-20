package hercules.api.services

import akka.actor.{ ActorRef }
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{ Await, duration, ExecutionContext }
import scala.util.{ Failure, Success }

import spray.http.StatusCodes._
import spray.routing.Directives

import hercules.protocols.HerculesMainProtocol._
import hercules.actors.masters.{ MasterState, MasterStateProtocol }
import hercules.entities.ProcessingUnit

class StatusService(cluster: ActorRef)(implicit executionContext: ExecutionContext) extends Directives {

  import duration._
  implicit val timeout = Timeout(5.seconds)
  val route =
    path("status") {
      get {
        detach() {
          complete {
            val request =
              cluster.ask(
                SendToAll(
                  "/user/master/active",
                  RequestMasterState())
              )
            val response = request.map {
              case Success(state) => {
                state match {
                  case s: MasterState => {
                    // @TODO Let some json marshaller handle the response instead
                    registerCustom(
                      200,
                      reason = "OK",
                      defaultMessage =
                        "messagesNotYetProcessed: {" +
                          s.messagesNotYetProcessed.map { _.unit.uri }.mkString(",") +
                          "}, messagesInProcessing: {" +
                          s.messagesInProcessing.map { _.unit.uri }.mkString(",") +
                          "} ,failedMessages: {" +
                          s.failedMessages.map { _.unit.uri }.mkString("}"))
                  }
                  case _ =>
                    InternalServerError
                }
              }
              case Failure(reason) =>
                InternalServerError
            }
            response
          }
        }
      }
    }
}
