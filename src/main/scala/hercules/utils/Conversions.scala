package hercules.utils

import java.io.File
import java.net.URI
import java.net.InetAddress

/**
 * Provide useful conversion
 */
object Conversions {

  // Make sure that the host name of the executing machine is parsed
  // into the URI info
  implicit def file2URI(file: File): URI = {
    val initialURI = file.toURI()
    val uriWithHost = new URI(
      initialURI.getScheme(),
      initialURI.getUserInfo(),
      InetAddress.getLocalHost().getHostName(),
      initialURI.getPort(),
      initialURI.getPath(),
      initialURI.getQuery(),
      initialURI.getFragment())
    uriWithHost
  }

}