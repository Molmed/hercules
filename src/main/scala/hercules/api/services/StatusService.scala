package hercules.api.services

import akka.actor.{ ActorRef }
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{ Await, duration, ExecutionContext }
import scala.util.{ Failure, Success }

import spray.http.StatusCodes._
import spray.routing._

import hercules.actors.masters.{ MasterState, MasterStateProtocol }
import hercules.api.{ Api, BootedCore, Core, CoreActors }
import hercules.entities.ProcessingUnit
import hercules.protocols.HerculesMainProtocol._

trait StatusService extends Directives {

  import duration._
  implicit val timeout: Timeout
  implicit val executionContext: ExecutionContext
  implicit val cluster: ActorRef

  def route = getRoute
  def getRoute = get {
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
}
