package hercules.utils

import java.io.File
import java.net.{ InetAddress, URI }

import org.scalatest.{ FlatSpec, Matchers }

class DeleteUtilsTest extends FlatSpec with Matchers {

  val file = new File("file")
  file.createNewFile()
  val directory = new File("dir")
  directory.mkdir()

  "It" should "be possible to delete files " in {

    DeleteUtils.deleteFilesAndDirs(Seq(file))
    assert(!file.exists(), "file should have been deleted, but was still there")
  }

  it should "possible to delete directories" in {
    DeleteUtils.deleteFilesAndDirs(Seq(directory))
    assert(!directory.exists(), "dir should have been deleted, but was still there")
  }

}