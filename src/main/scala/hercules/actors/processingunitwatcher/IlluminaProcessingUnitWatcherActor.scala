package hercules.actors.processingunitwatcher

import hercules.config.processing.ProcessingUnitWatcherConfig

/**
 * Base class for Actors which are watching for finished illumina runfolders.
 */
abstract class IlluminaProcessingUnitWatcherActor(
  config: ProcessingUnitWatcherConfig)
    extends ProcessingUnitWatcherActor(config) {

}