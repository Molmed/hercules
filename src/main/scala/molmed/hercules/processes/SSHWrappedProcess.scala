package molmed.hercules.processes

trait SSHWrappedProcess {

  def sshWrapper(
    username: Option[String] = None,
    hostname: String,
    command: String): String = {

    val userString = if (username.isDefined) username + "@" else ""
    "ssh " + userString + hostname + " " + command

  }

}