package hercules.api.services

import akka.actor.{ ActorRef, Actor }
import scala.concurrent._
import spray.routing._
import akka.pattern.ask
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.util.Timeout
import scala.concurrent.duration._
import hercules.protocols.HerculesMainProtocol._
import hercules.actors.masters.{MasterState,MasterStateProtocol}

class DemultiplexingService(cluster: ActorRef)(implicit executionContext: ExecutionContext) extends Directives {

  implicit val timeout = Timeout(5.seconds)
  import MasterStateProtocol._
  
  val route =
    pathPrefix( "demultiplex" ) {
      pathPrefix ( "failed" ) 
          restartFailedDemultiplexJob ~ 
          removeFailedDemultiplexJob ~
          addFailedDemultiplexJob
  }
  
  def restartFailedDemultiplexJob =
    path( "restart" / Segment ) { id => 
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
  
  def removeFailedDemultiplexJob =
    path( "remove" / Segment ) { id => 
      delete {
        detach() {
          complete {
            val state = Await.result(cluster.ask(SendToAll("/user/master/active", RequestMasterState(Option(id)))),timeout.duration).asInstanceOf[MasterState]
            val matchingMessage = state.failedMessages.find(x => x.unit.name == id).get
            cluster.tell(SendToAll("/user/master/active", RemoveFromFailedMessages(matchingMessage)),Actor.noSender)
            s"ProcessingUnit $id removed from FailedMessages" 
          }
        }
      }
    }
  
  def addFailedDemultiplexJob = 
    path( "add" / Segment ) { id => 
      post {
        detach() {
          complete {
//            cluster.tell(
//                SendToAll("/user/master/active", 
//                    AddToFailedMessages(
//                        ForgetDemultiplexingProcessingUnitMessage(id))),
//                Actor.noSender)
            s"Could not add FailedDemultiplexJob for $id : Not implemented" 
          }
        }
      }
    }
}
