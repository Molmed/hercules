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
              case MasterState(messagesNotYetProcessed, messagesInProcessing, failedMessages) => {
                // TODO Let some json marshaller handle the response instead
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
}
