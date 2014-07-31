package molmed.hercules.processes

import molmed.hercules.Runfolder

case class WriteTestFileToDirectoryProcess(runfolder: Runfolder)
    extends BiotankProcess {
  val command = "touch " + runfolder.runfolder + "/testfile"
}