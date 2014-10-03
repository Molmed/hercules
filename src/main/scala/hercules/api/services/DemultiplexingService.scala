package hercules.api.services

import akka.actor.{ActorRef}
import scala.concurrent.ExecutionContext
import spray.routing.Directives
import akka.pattern.ask
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.util.Timeout
import scala.concurrent.duration._
import hercules.protocols.HerculesMainProtocol._

class DemultiplexingService(cluster: ActorRef)(implicit executionContext: ExecutionContext) extends Directives {

  implicit val timeout = Timeout(5.seconds)
  val route = 
    path("demultiplex" / Segment) { id => 
      get {
          detach() {
            complete {(cluster ? SendToAll("/user/master/active",RestartDemultiplexingProcessingUnitMessage(id))).map {
              case Acknowledge => 
                "Master acknowledged request"
              case Reject(reason) =>  
                "Master rejected request"
            }
          }
          }
        }
    }
  
}
