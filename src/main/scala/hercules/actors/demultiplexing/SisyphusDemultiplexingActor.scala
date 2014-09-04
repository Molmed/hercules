package hercules.actors.demultiplexing

import akka.actor.Props
import hercules.actors.HerculesActor

object SisyphusDemultiplexingActor {
  def props(): Props = Props(new SisyphusDemultiplexingActor())
}

/**
 * Concrete executor implementation for demultiplexing using Sisyphus
 * This one can lock while doing it work.
 */
class SisyphusDemultiplexingActor extends HerculesActor {
  
  def receive = ???

}