package hercules.config.processingunit

import java.io.File
import akka.event.LoggingAdapter

/**
 * Provide a config for a IlluminaProcessingUnitFetcherConfig.
 *
 * @param runfolderRoots
 * @param sampleSheetRoot
 * @param customQCConfigRoot
 * @param defaultQCConfigFile
 * @param customProgramConfigRoot
 * @param defaultProgramConfigFile
 * @param log
 */
case class IlluminaProcessingUnitFetcherConfig(
  runfolderRoots: Seq[File],
  sampleSheetRoot: File,
  customQCConfigRoot: File,
  defaultQCConfigFile: File,
  customProgramConfigRoot: File,
  defaultProgramConfigFile: File,
  log: LoggingAdapter) extends ProcessingUnitFetcherConfig {}