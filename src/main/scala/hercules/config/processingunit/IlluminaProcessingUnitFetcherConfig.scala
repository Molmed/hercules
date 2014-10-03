package hercules.config.processingunit

import java.io.File
import akka.event.LoggingAdapter

case class IlluminaProcessingUnitFetcherConfig(
  runfolderRoot: File,
  sampleSheetRoot: File,
  customQCConfigRoot: File,
  defaultQCConfigFile: File,
  customProgramConfigRoot: File,
  defaultProgramConfigFile: File,
  log: LoggingAdapter) extends ProcessingUnitFetcherConfig {}