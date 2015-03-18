package hercules.actors.notifiers.rest.slack

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{ FlatSpecLike }

/**
 * Created by johda411 on 2015-03-18.
 *
 * A very basic smoke test to make sure the SlackNotifierActor starts without throwing exceptions.
 */
class SlackNotifierActorTest extends TestKit(ActorSystem("SlackNotifierActorTestSystem"))
    with FlatSpecLike {

  "An SlackNotifierActor" should "start successfully" in {
    SlackNotifierActor(system)
  }

}
