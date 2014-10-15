package hercules.api.services

import akka.actor.{ ActorRef, Actor }
import scala.concurrent._
import spray.routing._
import spray.http.StatusCodes._
import akka.pattern.ask
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.util.Timeout
import scala.concurrent.duration._
import hercules.protocols.HerculesMainProtocol._
import hercules.actors.masters.{ MasterState, MasterStateProtocol }

class DemultiplexingService(cluster: ActorRef)(implicit executionContext: ExecutionContext) extends Directives {

  implicit val timeout = Timeout(5.seconds)
  import MasterStateProtocol._

  val route =
    pathPrefix("demultiplex" / Segment) { id =>
      restartFailedDemultiplexJob(id) ~
        removeFailedDemultiplexJob(id) ~
        forgetDemultiplexJob(id) ~
        stopRunningDemultiplexJob(id)
    }

  def restartFailedDemultiplexJob(id: String) =
    path("restart") {
      put {
        complete {
          (cluster ? SendToAll("/user/master/active", RestartDemultiplexingProcessingUnitMessage(id))).map {
            case Acknowledge =>
              Accepted
            case Reject(reason) =>
              NotFound
          }
        }
      }
    }

  def removeFailedDemultiplexJob(id: String) =
    path("remove") {
      delete {
        detach() {
          complete {
            // @TODO handle timeouts and missing messages
            val state = Await.result(cluster.ask(SendToAll("/user/master/active", RequestMasterState(Option(id)))), timeout.duration).asInstanceOf[MasterState]
            val matchingMessage = state.failedMessages.find(x => x.unit.name == id)
            if (!matchingMessage.isEmpty) {
              cluster.tell(SendToAll("/user/master/active", RemoveFromFailedMessages(matchingMessage.get)), Actor.noSender)
              OK
            } else {
              NotFound
            }
          }
        }
      }
    }

  def forgetDemultiplexJob(id: String) =
    path("forget") {
      delete {
        detach() {
          complete {
            cluster.tell(
              SendToAll("/user/master/active",
                ForgetDemultiplexingProcessingUnitMessage(id)),
              Actor.noSender)
            NotImplemented
          }
        }
      }
    }

  def stopRunningDemultiplexJob(id: String) =
    path("stop") {
      put {
        detach() {
          complete {
            cluster.tell(
              SendToAll("/user/master/active", StopDemultiplexingProcessingUnitMessage(id)),
              Actor.noSender)
            NotImplemented
          }
        }
      }
    }

}
