package molmed.hercules.processes

import java.io.File
import molmed.hercules.Runfolder
import molmed.hercules.ProcessingState

trait BiotankProcess extends RunfolderProcess with SSHWrappedProcess {

  val runfolder: Runfolder
  val command: String

  def getBiotankToRunOnFromFolderStructure(file: File): String = {
    val regexp = """(biotank\d+)""".r
    regexp.findFirstIn(file.getAbsolutePath()).
      getOrElse(throw new Exception("Couldn't resolve biotank destination " +
        "from folder structure."))
  }

  override def start(): Runfolder = {
    import scala.sys.process._

    val biotankToRunOn =
      getBiotankToRunOnFromFolderStructure(runfolder.runfolder)

    val commandToRun = sshWrapper(hostname = biotankToRunOn, command = command)

    val exitStatus = commandToRun.!

    if (exitStatus == 0)
      new Runfolder(runfolder.runfolder,
        runfolder.samplesheet,
        ProcessingState.Finished)
    else
      throw new Exception(
        "Command: " + commandToRun + " exited with non-zero exit status!")
  }

}