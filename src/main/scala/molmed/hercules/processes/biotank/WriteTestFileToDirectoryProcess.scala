package molmed.hercules.processes.biotank

import molmed.hercules.Runfolder

case class WriteTestFileToDirectoryProcess(runfolder: Runfolder)
    extends BiotankProcess {
  val command = "hostname > " + runfolder.runfolder + "/testfile"
}