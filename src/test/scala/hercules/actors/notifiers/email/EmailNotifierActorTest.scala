package hercules.actors.notifiers.email

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FlatSpecLike

/**
 * Smoke test for the EmailNotifierActor - currently only makes sure that it starts
 * without throwing any exceptions. Testing of any actual behaviour should be moved
 * into the sub traits/classes.
 */
class EmailNotifierActorTest()
    extends TestKit(ActorSystem("EmailNotifierActorTestSystem"))
    with FlatSpecLike {

  "An EmailNotifierActor" should "start successfully" in {
    EmailNotifierActor(system)
  }

}