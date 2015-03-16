package hercules.actors.notifiers.email

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FlatSpecLike

class EmailNotifierActorTest()
    extends TestKit(ActorSystem("EmailNotifierActorTestSystem"))
    with FlatSpecLike {

  "An EmailNotifierActor" should "start successfully" in {
    EmailNotifierActor(system)
  }

}