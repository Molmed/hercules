package hercules.external.program

import hercules.entities.ProcessingUnit
import java.io.File
import java.io.PrintWriter
import scala.sys.process.ProcessIO
import java.io.ByteArrayOutputStream
import org.apache.commons.io.FileUtils

class Sisyphus() extends ExternalProgram {

  //@TODO Make this configurable!
  val sisyphusInstallLocation = "/vagrant/test_system/sisyphus/"
  val sisyphusLogLocation = "/vagrant/test_system/sislogs/"

  def run(unit: ProcessingUnit): (Boolean, File) = {

    import scala.sys.process._

    val runfolder = new File(unit.uri)
    val logFile = new File(sisyphusLogLocation + "/" + runfolder.getName() + ".log")

    val command =
      sisyphusInstallLocation +
        "sisyphus.pl " +
        " -runfolder " + runfolder.getAbsolutePath() +
        " -nowait "

    val writer = new PrintWriter(logFile)

    val exitStatus = command.!(ProcessLogger(writer.println, writer.println))

    writer.close()

    (exitStatus == 0, logFile)
  }

  /**
   * Remove all the sisyphus folders and files which should be removed!
   */
  def cleanup(unit: ProcessingUnit): Unit = {

    val runfolder = new File(unit.uri)
    val runfolderName = runfolder.getName()

    val filesAndDirsToDelete = Seq(
      "Sisyphus",
      "MD5",
      "rsync*.log",
      "sisyphus.sh",
      "excludedTiles.yml",
      "/data/scratch/" + runfolderName,
      "Unaligned",
      "Excluded",
      "quickReport.xml",
      "setupBclToFastq.err")

    filesAndDirsToDelete.map(x => new File(runfolder + "/" + x)).foreach(x => {
      if (x.exists())
        if (x.isDirectory())
          FileUtils.deleteQuietly(x)
        else
          x.delete()
    })
  }
}