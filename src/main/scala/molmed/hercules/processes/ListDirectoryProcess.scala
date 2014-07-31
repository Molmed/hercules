package molmed.hercules.processes

import molmed.hercules.Runfolder

case class ListDirectoryProcess(runfolder: Runfolder) extends BiotankProcess {
  val command = "ls " + runfolder.runfolder.getAbsolutePath() 
}