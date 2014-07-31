package molmed.hercules

import java.io.File

import ProcessingState._

case class Runfolder(runfolder: File,
                     samplesheet: File,
                     state: ProcessingState)