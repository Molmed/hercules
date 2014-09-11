package hercules.actors.notifiers

import akka.actor.Props
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import java.util.Hashtable
import java.util.ArrayList
import java.net.InetAddress
import hercules.actors.HerculesActor


object EmailNotifierExecutorActor {
  
  /**
     * Factory method for creating a EmailNotifierExecutorActor
     * Loads it's configuration from the NotifierActor.conf
     * @param configFile the configFile to load
     * @returns a Props of EmailNotifierExecutorActor
     */
  def props(configFile: String = "NotifierActor"): Props = {

    // Parse email options from the configuration and substitute missing values with the defaults
    val conf = ConfigFactory.load(configFile).withFallback(defaults)
    //try { 
      val emailConfig = conf.getConfig("email")
    //} catch {
    //  case e: ConfigException.Missing => log.error("Could not find email configuration")
    //  case e: ConfigException.WrongType => log.error("Error parsing email configuration")
    //} 
    val recipients = emailConfig.getStringList("recipients")
    val smtpHost = emailConfig.getString("smtp_host")
    val smtpPort = emailConfig.getInt("smtp_port")
    val sender = emailConfig.getString("sender")
    val prefix = emailConfig.getString("prefix")
    Props(new EmailNotifierExecutorActor(
      recipients,
      sender,
      prefix,
      smtpHost,
      smtpPort
    ))
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
  
}

class EmailNotifierExecutorActor(
  val emailRecipients: java.util.List[String],
  val emailSender: String,
  val emailPrefix: String,
  val emailSmtpHost: String,
  val emailSmtpPort: Int) extends HerculesActor {
    
    def receive = ???
} 