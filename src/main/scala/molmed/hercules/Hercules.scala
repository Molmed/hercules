package molmed.hercules

import akka.actor.{ ActorRef, ActorSystem, Props, Actor, Inbox }
import akka.event.Logging
import scala.concurrent.duration._
import java.io.File
import ProcessingState._
import molmed.hercules.messages._

class Master() extends Actor with akka.actor.ActorLogging {

  //@TODO Make configurable
  val runfolders = Seq(
    new File("/seqdata/biotank1/runfolders"),
    new File("/seqdata/biotank2/runfolders"))
  val samplesheets = new File("/srv/samplesheet/Processning/")

  log.info("Master starting")

  val runfolderWatcher = context.actorOf(
    Props(new RunfolderWatcher(runfolders, samplesheets)),
    "RunfolderWatcher")

  val runfolderProcessor = context.actorOf(
    Props(new RunfolderProcessor()),
    "RunfolderProcessor")

  runfolderWatcher ! StartMessage()

  def receive = {
    case message: ProcessRunFolderMessage => {

      log.info("Got a ProcessRunFolderMessage")

      message.runfolder.state match {
        case ProcessingState.Found =>
          log.info("Found it!")
          runfolderProcessor ! message
        case ProcessingState.Running =>
          log.info("It's running!")
        case ProcessingState.Finished =>
          log.info("And it's finished!")
          self ! StopMessage
      }

    }
    case StopMessage => {
      context.system.shutdown()
    }
  }

}

object Hercules extends App {

  // Create the 'hercules' actor system
  val system = ActorSystem("hercules")

  // Create the 'master' actor
  val master = system.actorOf(
    Props(new Master()),
    "master")

  system.awaitTermination()

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
