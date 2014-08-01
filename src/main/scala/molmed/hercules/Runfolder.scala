package molmed.hercules

import java.io.File

import ProcessingState._

//@TODO This should probably know it's remote location (on uppmax) to
case class Runfolder(runfolder: File,
                     samplesheet: File,
                     state: ProcessingState)