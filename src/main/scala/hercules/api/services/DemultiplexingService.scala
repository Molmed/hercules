package hercules.api.services

import akka.actor.{ ActorRef, Actor }
import scala.concurrent._
import spray.routing._
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
              "Master acknowledged request"
            case Reject(reason) =>
              "Master rejected request"
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
            val matchingMessage = state.failedMessages.find(x => x.unit.name == id).get
            cluster.tell(SendToAll("/user/master/active", RemoveFromFailedMessages(matchingMessage)), Actor.noSender)
            s"ProcessingUnit $id removed from FailedMessages"
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
            s"Master was told to forget previous processing of ProcessingUnit $id"
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
            s"Master was told to stop processing of ProcessingUnit $id"
          }
        }
      }
    }

}
