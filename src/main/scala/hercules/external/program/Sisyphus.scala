package hercules.external.program

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.Date

import scala.sys.process.ProcessLogger
import scala.sys.process.stringToProcess

import org.apache.commons.io.FileUtils

import com.typesafe.config.ConfigFactory

import hercules.demultiplexing.Demultiplexer
import hercules.demultiplexing.DemultiplexingResult
import hercules.entities.ProcessingUnit
import hercules.utils.Formats

class Sisyphus() extends Demultiplexer with ExternalProgram {

  val config = ConfigFactory.load()
  val sisyphusInstallLocation = config.getString("general.sisyphusInstallLocation")
  val sisyphusLogLocation = config.getString("general.sisyphusLogLocation")

  def demultiplex(unit: ProcessingUnit): DemultiplexingResult = {
    val (exitStatus, logFile) = run(unit)
    new DemultiplexingResult(exitStatus, Some(logFile))
  }

  def run(unit: ProcessingUnit): (Boolean, File) = {

    import scala.sys.process._

    val runfolder = new File(unit.uri)
    val logFile = new File(sisyphusLogLocation + "/" + runfolder.getName() + ".log")

    val command =
      sisyphusInstallLocation +
        "sisyphus.pl " +
        " -runfolder " + runfolder.getAbsolutePath() +
        " -nowait "

    val writer =
      new PrintWriter(
        new FileWriter(logFile, true))

    // Drop a time stamp for the sisyphus run attempt.
    writer.println("--------------------------------------------------")
    writer.println(Formats.date.format(new Date()))
    writer.println("--------------------------------------------------")

    def writeAndFlush(s: String) = {
      writer.println(s)
      writer.flush()
    }

    val exitStatus = command.!(ProcessLogger({ s => writeAndFlush(s) }, { s => writeAndFlush(s) }))

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