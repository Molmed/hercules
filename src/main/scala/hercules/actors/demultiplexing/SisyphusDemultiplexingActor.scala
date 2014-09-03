package hercules.actors.demultiplexing

import akka.actor.Props

object SisyphusDemultiplexingActor {
  def props(): Props = Props(new SisyphusDemultiplexingActor())
}

/**
 * Concrete implementation for demultiplexing using Sisyphus
 */
class SisyphusDemultiplexingActor extends IlluminaDemultiplexingActor {

  def receive = ???
  
}