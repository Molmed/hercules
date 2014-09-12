package hercules.config.notification

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.util.Hashtable
import java.util.List

object EmailNotificationConfig {

  /** 
    * Return a ConfigFactory.Config object with default email settings which can be 
    * overridden with settings from the config file
  */
  def defaults(): Config = {
    val defaultSettings = new java.util.Hashtable[String,Object]()
    //defaultSettings.put("email.recipients",new java.util.ArrayList[String]().subList(0,0))
    defaultSettings.put("smtp_host","localhost")
    defaultSettings.put("smtp_port",new java.lang.Integer(25))
    defaultSettings.put("sender",this.getClass.getName + "@" + java.net.InetAddress.getLocalHost.getHostName)
    defaultSettings.put("prefix","[Hercules]")
    ConfigFactory.parseMap(defaultSettings,"default email settings")
  }
  
  /** Create and return a EmailNotificationConfig from the supplied 
   *  configuration. If necessary, provide default values for missing options.
   */
  def getEmailNotificationConfig(emailConf: Config): EmailNotificationConfig = {
    val conf = emailConf.withFallback(defaults)
    println(conf.root.render)
    val emailRecipients = conf.getStringList("recipients")  
    val emailSender = conf.getString("sender")
    val emailSMTPHost = conf.getString("smtp_host")
    val emailSMTPPort = conf.getInt("smtp_port")
    val emailPrefix = conf.getString("prefix")
    new EmailNotificationConfig(
      emailRecipients,
      emailSender,
      emailSMTPHost,
      emailSMTPPort,
      emailPrefix
    )
  }
  
}

/**
 * Base class for configuring an email notification
 */
case class EmailNotificationConfig(
  val emailRecipients: java.util.List[String],
  val emailSender: String,
  val emailSMTPHost: String,
  val emailSMTPPort: Int,
  val emailPrefix: String) extends NotificationConfig {
}
