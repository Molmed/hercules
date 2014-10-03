package hercules.external.program

import hercules.entities.ProcessingUnit
import java.io.File

trait ExternalProgram {

  /**
   * Execute program, tuple with (successful run, log file).
   * @param unit
   * @return Tuppel with info on if run was successfully run and the log file.
   */
  def run(unit: ProcessingUnit): (Boolean, File)

  /**
   * Run any side effects that need to be run after a failed command.
   */
  def cleanup(unit: ProcessingUnit): Unit
}