package molmed.hercules.processes

import molmed.hercules.Runfolder

trait RunfolderProcess {

  val runfolder: Runfolder
  def start(): Runfolder

}