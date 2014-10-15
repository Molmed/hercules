package hercules.api.services

import akka.actor.{ ActorRef }
import scala.concurrent.{ ExecutionContext, Await }
import spray.routing.Directives
import akka.pattern.ask
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.util.Timeout
import scala.concurrent.duration._

import hercules.protocols.HerculesMainProtocol._
import hercules.actors.masters.{ MasterState, MasterStateProtocol }
import hercules.entities.ProcessingUnit

class StatusService(cluster: ActorRef)(implicit executionContext: ExecutionContext) extends Directives {

  implicit val timeout = Timeout(5.seconds)
  val route =
    path("status") {
      get {
        detach() {
          complete {
            val state =
              Await.result(
                cluster.ask(
                  SendToAll(
                    "/user/master/active",
                    RequestMasterState())),
                timeout.duration).asInstanceOf[MasterState]
            // @TODO Let some json marshaller handle the response instead
            "messagesNotYetProcessed: {" +
              state.messagesNotYetProcessed.map { _.unit.uri }.mkString(",") +
              "}, messagesInProcessing: {" +
              state.messagesInProcessing.map { _.unit.uri }.mkString(",") +
              "} ,failedMessages: {" +
              state.failedMessages.map { _.unit.uri }.mkString("}")
          }
        }
      }
    }
}
