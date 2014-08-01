package molmed.hercules.processes

import molmed.hercules.Runfolder
import molmed.hercules.processes.biotank.BiotankProcess

case class ListDirectoryProcess(runfolder: Runfolder) extends BiotankProcess {
  val command = "ls " + runfolder.runfolder.getAbsolutePath() 
}