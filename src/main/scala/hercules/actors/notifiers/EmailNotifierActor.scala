package hercules.actors.notifiers

import akka.actor.Props

object EmailNotifierActor {
  def props(): Props = Props(new EmailNotifierActor())
}

class EmailNotifierActor extends NotifierActor {

}