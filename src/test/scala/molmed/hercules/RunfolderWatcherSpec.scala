package molmed.hercules

import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers }
import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit, TestActorRef }
import scala.concurrent.duration._
import java.io.File
import molmed.hercules.messages.ProcessRunFolderMessage
import molmed.hercules.messages.StartMessage

class RunfolderWatcherSpec(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with Matchers
    with FlatSpecLike
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  def this() = this(ActorSystem("RunfolderWatcherSpec"))

  val generalWaitTime = 10.seconds

  val runfolders = Seq(new File("src/test/resources/runfolders/"))
  val samplesheets = new File("src/test/resources/processing")

  val expectedRunfolder = new Runfolder(
    new File("src/test/resources/runfolders/140708_D00458_0025_AC493LACX"),
    new File("src/test/resources/processing/" +
      "140708_D00458_0025_AC493LACX_samplesheet.csv"),
    ProcessingState.Found)

  val foundFile = new File("src/test/resources/runfolders/140708_D00458_0025_AC493LACX/.found")

  val recentlyCreatedFile = new File("src/test/resources/runfolders/140708_D00458_0025_AC493LACX/recent")

  def deleteTmpFiles(): Unit = {
    foundFile.delete()
    recentlyCreatedFile.delete()
  }

  override def beforeEach() {
    deleteTmpFiles()
  }

  override def afterEach() {
    deleteTmpFiles()
  }

  override def afterAll: Unit = {
    deleteTmpFiles()
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  "A RunfolderWatcher" should " looking for runfolders on StartMessage" in {

    val expectedMessage = ProcessRunFolderMessage(expectedRunfolder)

    val runfolderWatcher = TestActorRef(Props(
      new RunfolderWatcher(runfolders, samplesheets)))
    runfolderWatcher ! StartMessage()

    val message = receiveN(1, 4.second)
    assert(message(0) === expectedMessage)
    assert(foundFile.exists())

  }
  //
  it should "not return runfolders which have already been found " +
    "(that is, a .found file is written in the folder)" in {

      val expectedMessage = ProcessRunFolderMessage(expectedRunfolder)

      val runfolderWatcher = TestActorRef(Props(
        new RunfolderWatcher(runfolders, samplesheets)))
      runfolderWatcher ! StartMessage()

      val message = receiveN(1, generalWaitTime)
      assert(message(0) === expectedMessage)
      assert(foundFile.exists())

    }

  it should "place a .found file in the runfolder after having found it." in {

    val expectedMessage = ProcessRunFolderMessage(expectedRunfolder)

    val runfolderWatcher = TestActorRef(Props(
      new RunfolderWatcher(runfolders, samplesheets)))
    runfolderWatcher ! StartMessage()

    val message = receiveN(1, generalWaitTime)
    assert(message(0) === expectedMessage)
    assert(foundFile.exists())
  }

  it should "not return runfolders which are currently being written to" +
    " (meaning that there has been a write event on a file in the " +
    "directory less than 1 h ago." in {

      recentlyCreatedFile.createNewFile()

      val expectedMessage = ProcessRunFolderMessage(expectedRunfolder)

      val runfolderWatcher = TestActorRef(Props(
        new RunfolderWatcher(runfolders, samplesheets)))
      runfolderWatcher ! StartMessage()

      expectNoMsg(generalWaitTime)

    }

  it should "only return a runfolder if the corresponding sample sheet is " +
    "found." in {

      // src/test/resources/runfolders/140708_D00458_0028_AC493LACX/ should
      // not be found.

      val expectedMessage = ProcessRunFolderMessage(expectedRunfolder)

      val runfolderWatcher = TestActorRef(Props(
        new RunfolderWatcher(runfolders, samplesheets)))
      runfolderWatcher ! StartMessage()

      val message = receiveN(1, generalWaitTime)
      assert(message(0) === expectedMessage)
    }
}

