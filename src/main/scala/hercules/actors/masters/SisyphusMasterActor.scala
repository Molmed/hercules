package hercules.actors.masters

import akka.actor.ActorContext
import akka.actor.Props

object SisyphusMasterActor {
  def props(): Props = Props(new SisyphusMasterActor())
}

/**
 * Defines the logic for running the Sisyphus workflow
 */
class SisyphusMasterActor extends HerculesMasterActor {

  def receive = ???

}