package hercules.actors.notifiers

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import akka.actor.ActorSystem
import akka.japi.Util.immutableSeq
import akka.actor.Props
import akka.actor.AddressFromURIString
import akka.actor.RootActorPath
import akka.contrib.pattern.ClusterClient
import akka.actor.ActorRef
import akka.contrib.pattern.ClusterClient.SendToAll
import java.net.InetAddress
import scala.concurrent.duration._


object EmailNotifierActor {

  /**
   * Initiate all the stuff needed to start a EmailNotifierActor
   * including initiating the system.
   */

  def startEmailNotifierActor(): Unit = {

    val hostname = InetAddress.getLocalHost().getHostName()

    val conf = ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostname").
      withFallback(ConfigFactory.load("EmailNotifierActor").withFallback(defaults))

    println(conf.root().render())
    val system = ActorSystem("NotificationSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) ⇒ system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    val props = EmailNotifierActor.props(clusterClient)
    system.actorOf(props, "email_notifier")
    val executor = system.actorOf(
      EmailNotifierExecutorActor.startEmailNotifierExecutorActor(
        conf.getConfig("email")
      ), "email_notifier_executor")
    
      executor ! "***Executor started***"
  }

  /** 
    * Return a ConfigFactory.Config object with default email settings which can be 
    * overridden with settings from the config file
  */
  def defaults(): Config = {
    val defaultSettings = new java.util.Hashtable[String,Object]()
    defaultSettings.put("email.recipients",new java.util.ArrayList[String]().subList(0,0))
    defaultSettings.put("email.smtp_host","localhost")
    defaultSettings.put("email.smtp_port",new java.lang.Integer(25))
    defaultSettings.put("email.sender",this.getClass.getName + "@" + java.net.InetAddress.getLocalHost.getHostName)
    defaultSettings.put("email.prefix","[Hercules]")
    ConfigFactory.parseMap(defaultSettings,"default email settings")
  }
  
  /**
   * Create a new EmailNotifierActor
   * @param clusterClient A reference to a cluster client thorough which the
   *                      actor will communicate with the rest of the cluster.
   */
  def props(clusterClient: ActorRef): Props = {
    Props(new EmailNotifierActor(clusterClient))
  }
  
}

class EmailNotifierActor(clusterClient: ActorRef) extends NotifierActor {
  
  clusterClient ! SendToAll("/user/master/active", "I'm alive")
  
  
  def receive = {
    case _ => log.info("EmailNotifierActor got a message!")
  }

}