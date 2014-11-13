package hercules.utils

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.io.File
import java.net.URI
import java.net.InetAddress

class ConversionsTest extends FlatSpec with Matchers {

  val file = new File("/test/file")
  val expected = "file://" + InetAddress.getLocalHost().getHostName() + "/test/file"

  "A file2URI conversion" should "correctly convert a file to a URI " in {

    val actual = Conversions.file2URI(file)
    assert(expected === actual.toString())

  }

  it should "do so implicitly if imported" in {

    import Conversions.file2URI

    val actual: URI = file
    assert(expected === actual.toString())

  }

}