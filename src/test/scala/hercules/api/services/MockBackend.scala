package hercules.api.services

import akka.actor.{ ActorRef, ActorSystem }
import akka.contrib.pattern.ClusterClient.SendToAll
import akka.testkit.{ TestActor, TestProbe }
import akka.util.Timeout

import hercules.actors.masters.MasterStateProtocol
import hercules.actors.masters.state.MasterState
import hercules.entities.ProcessingUnit
import hercules.protocols.HerculesMainProtocol._

import java.io.File
import java.net.URI

import hercules.test.utils.ProcessingUnitPlaceholder

import scala.concurrent.ExecutionContext

object MockBackend {
  def apply(
    system: ActorSystem,
    masterState: MasterState,
    doNotAnswer: Boolean = false): TestProbe =

    new MockBackend(
      system,
      masterState,
      doNotAnswer)

}

class MockBackend(
    _application: ActorSystem,
    var state: MasterState,
    doNotAnswer: Boolean = false) extends TestProbe(_application: ActorSystem) {

  import MasterStateProtocol._

  // Install an AutoPilot to generate responses on request from the ApiServices
  setAutoPilot(
    new TestActor.AutoPilot {
      def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {

        msg match {
          case SendToAll(_, message) => {

            val response =
              message match {

                case RestartDemultiplexingProcessingUnitMessage(id) => {
                  if (state.findStateOfUnit(Some(id)).failedMessages.isEmpty)
                    Reject(Some(s"$id not in failedMessages set"))
                  else
                    Acknowledge
                }

                case RequestMasterState(unit) => {
                  if (unit.isDefined)
                    state.findStateOfUnit(unit)
                  else
                    state
                }

                case m: RemoveFromFailedMessages => {
                  state = state.manipulateState(m)
                }

                case ForgetDemultiplexingProcessingUnitMessage(id) => {

                  val stateOfUnit = state.findStateOfUnit(Some(id))
                  // Find state to change
                  state = state.copy(messagesInProcessing = state.messagesInProcessing.filterNot(stateOfUnit.messagesInProcessing))

                  if (state.findStateOfUnit(Some(id)).messagesInProcessing.isEmpty) Acknowledge
                  else Reject(Some(s"Processing Unit $id is being processed"))
                }

              }

            if (!doNotAnswer) {
              sender ! response
            }

            TestActor.KeepRunning
          }

          case m: RemoveFromFailedMessages => {
            state = state.manipulateState(m)
            TestActor.KeepRunning
          }

          case _ => {
            println("We are on no autopilot case!")
            TestActor.NoAutoPilot
          }

        }
      }
    }
  )

}
