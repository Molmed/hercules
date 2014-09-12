package hercules.actors.notifiers

import akka.actor.Props
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import java.util.Hashtable
import java.util.ArrayList
import java.net.InetAddress
import hercules.actors.HerculesActor


object EmailNotifierExecutorActor {
  
  def startEmailNotifierExecutorActor(conf: Config): Props = {
    EmailNotifierExecutorActor.props(conf)
  }
  
  def props(conf: Config): Props = {
    Props(new EmailNotifierExecutorActor(conf))
  }
  
  def sendMessage(emailMsg: String, emailRecipients: java.util.List[String], emailSender: String) {
    println("To: " + emailRecipients.toString() + ", From: " + emailSender + ", Msg: " + emailMsg)
  }
  
}

class EmailNotifierExecutorActor(
  conf: Config) extends HerculesActor {
  
  val emailRecipients = conf.getStringList("recipients")  
  val emailSender = conf.getString("sender")
  val emailSmtpHost = conf.getString("smtp_host")
  val emailSmtpPort = conf.getInt("smtp_port")
  val emailPrefix = conf.getString("prefix")
  
  def receive = {
    case message => EmailNotifierExecutorActor.sendMessage(message.toString(),emailRecipients,emailSender)
  }
} 