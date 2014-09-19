package hercules.test.utils

import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.contrib.pattern.ClusterReceptionistExtension

object FakeMaster {
  case class MasterWrapped(msg: Any)
  def props(testProbe: ActorRef): Props = Props(new FakeMaster(testProbe))
}

class FakeMaster(testProbe: ActorRef) extends Actor with ActorLogging {
  // The master will register it self to the cluster receptionist.
  ClusterReceptionistExtension(context.system).registerService(self)

  def receive() = {
    case msg => {
      testProbe.tell(FakeMaster.MasterWrapped(msg), sender)
    }
  }
}