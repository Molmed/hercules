package hercules.actors

import akka.actor.Actor

/** 
 *  The base class for all Hercules actors. All actors (which are not 
 *  spinned up in a very local context, e.g. annonymous actors) should extend
 *  this class. 
 */
abstract class HerculesActor extends Actor {

}