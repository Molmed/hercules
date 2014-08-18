package molmed.hercules

import akka.actor.{ ActorSystem, Props }

object Hercules extends App {

  // Create the 'hercules' actor system
  val system = ActorSystem("hercules")

  // Create the 'master' actor
  val master = system.actorOf(
    Props(new Master()),
    "master")

  system.awaitTermination()

  //@TODO Remove this in the future!
  //master ! StartMessage

  //  // Create an "actor-in-a-box"
  //  val inbox = Inbox.create(system)
  //
  //  runfolderWatcher ! TestMessage

  // Ask the 'greeter for the latest 'greeting'
  // Reply should go to the "actor-in-a-box"
  //inbox.send(greeter, Greet)

  // Wait 5 seconds for the reply with the 'greeting' message
  //val Greeting(message1) = inbox.receive(5.seconds)
  //println(s"Greeting: $message1")

  // Change the greeting and ask for it again
  //greeter.tell(WhoToGreet("typesafe"), ActorRef.noSender)
  //inbox.send(greeter, Greet)
  //val Greeting(message2) = inbox.receive(5.seconds)
  //println(s"Greeting: $message2")

  //val greetPrinter = system.actorOf(Props[GreetPrinter])
  // after zero seconds, send a Greet message every second to the greeter with a sender of the greetPrinter
  //system.scheduler.schedule(0.seconds, 1.second, greeter, Greet)(system.dispatcher, greetPrinter)

}
