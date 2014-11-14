package hercules.external.program

import hercules.entities.ProcessingUnit
import java.io.File

/**
 * Base trait for the external programs
 */
trait ExternalProgram {

  /**
   * Execute program, tuple with (successful run, log file).
   * @param unit
   * @return Tuppel with info on if run was successfully run and the log file.
   */
  def run(unit: ProcessingUnit): (Boolean, File)

  /**
   * Run any side effects that need to be run after a failed command.
   * @param unit to perform clean-up procedure on.
   */
  def cleanup(unit: ProcessingUnit): Unit
}