package hercules.api.services

import akka.actor.{ ActorRef, Actor }
import scala.concurrent._
import spray.routing._
import spray.http.StatusCodes._
import akka.pattern.ask
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.util.Timeout
import scala.util.{ Success, Failure }
import hercules.protocols.HerculesMainProtocol._
import hercules.actors.masters.{ MasterState, MasterStateProtocol }

class DemultiplexingService(cluster: ActorRef)(implicit executionContext: ExecutionContext) extends Directives {

  import duration._
  implicit val timeout = Timeout(5.seconds)
  import MasterStateProtocol._

  val route =
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
  def restartFailedDemultiplexJob(id: String) =
    path("restart") {
      put {
        detach() {
          complete {
            cluster.ask(
              SendToAll(
                "/user/master/active",
                RestartDemultiplexingProcessingUnitMessage(id))
            ).map {
                case Acknowledge =>
                  Accepted
                case Reject(reason) =>
                  NotFound
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
  def removeFailedDemultiplexJob(id: String) =
    path("remove") {
      delete {
        detach() {
          complete {
            val request =
              cluster.ask(
                SendToAll(
                  "/user/master/active",
                  RequestMasterState(Some(id)))
              )
            val response = request.map {
              case Success(state) => {
                state match {
                  case s: MasterState => {
                    val matchingMessage = s.failedMessages.find(x => x.unit.name == id)
                    if (!matchingMessage.isEmpty) {
                      cluster.tell(SendToAll("/user/master/active", RemoveFromFailedMessages(matchingMessage)), Actor.noSender)
                      OK
                    } else {
                      NotFound
                    }
                  }
                  case _ =>
                    InternalServerError
                }
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

  /**
   * Forget any previous demultiplex results for the specified unit.
   * This should make it discoverable again by the
   * ProcessUnitWatcher and trigger a new demultiplexing job
   * @TODO Implement this functionality in master
   */
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

  /**
   * Stop an ongoing demultiplexing job on the specified unit
   * @TODO Implement this functionality
   */
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
