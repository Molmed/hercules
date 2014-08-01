package molmed.hercules.processes

import molmed.hercules.Runfolder

trait RunfolderProcess {

  val runfolder: Runfolder
  val command: String
  def start(): Runfolder

}