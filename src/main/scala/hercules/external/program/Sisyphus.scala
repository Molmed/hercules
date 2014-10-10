package hercules.external.program

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.Date

import scala.sys.process.ProcessLogger
import scala.sys.process.stringToProcess
import scala.concurrent._

import org.apache.commons.io.FileUtils

import com.typesafe.config.ConfigFactory

import hercules.demultiplexing.Demultiplexer
import hercules.demultiplexing.DemultiplexingResult
import hercules.entities.ProcessingUnit
import hercules.utils.Formats

import hercules.entities.illumina.HiSeqProcessingUnit
import hercules.entities.illumina.MiSeqProcessingUnit

class Sisyphus() extends Demultiplexer with ExternalProgram {

  val config = ConfigFactory.load()
  val sisyphusInstallLocation = config.getString("general.sisyphusInstallLocation")
  val sisyphusLogLocation = config.getString("general.sisyphusLogLocation")

  def demultiplex(unit: ProcessingUnit)(implicit executor: ExecutionContext): Future[DemultiplexingResult] = {
    future {
      // Do a cleanup before attempting to start demultiplexing
      cleanup(unit)
      val (success, logFile) = run(unit)
      new DemultiplexingResult(success, Some(logFile))
    }
  }

  def run(unit: ProcessingUnit): (Boolean, File) = {

    val runfolder = new File(unit.uri)
    val logFile = new File(sisyphusLogLocation + "/" + runfolder.getName() + ".log")

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

    val (processingUnitConfig, command) = unit match {
      case highSeq: HiSeqProcessingUnit => (Some(highSeq.processingUnitConfig),
        Some(sisyphusInstallLocation + "sisyphus.pl " + " -runfolder " +
          runfolder.getAbsolutePath() + " -nowait "))
      case miSeq: MiSeqProcessingUnit => (Some(miSeq.processingUnitConfig),
        Some(sisyphusInstallLocation + "sisyphus.pl " +
          (if (miSeq.performeOnMachineAnalysis) "-miseq" else "") +
          " -runfolder " + runfolder.getAbsolutePath() + " -nowait "))
      case _ => (None, None)
    }

    import scala.sys.process._

    val exitStatus = if (command.isDefined) {
      FileUtils.copyFile(processingUnitConfig.get.programConfig.get, new File(runfolder + "/sisyphus.yml"))
      FileUtils.copyFile(processingUnitConfig.get.QCConfig, new File(runfolder + "/sisyphus_qc.xml"))
      FileUtils.copyFile(processingUnitConfig.get.sampleSheet, new File(runfolder + "/SampleSheet.csv"))

      val proc = command.get.run(ProcessLogger({ s => writeAndFlush(s) }, { s => writeAndFlush(s) }))
      proc.exitValue
    } else 1
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
