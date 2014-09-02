package hercules.actors.processingunitwatcher

import hercules.config.processing.ProcessingUnitWatcherConfig

/**
 * Base class for Actors which are watching for finished illumina runfolders.
 * //@TODO I'm not so sure that this class should really be abstact.
 *         Maybe we should provide the concrete implementation in here?
 */
abstract class IlluminaProcessingUnitWatcherActor(
  config: ProcessingUnitWatcherConfig)
    extends ProcessingUnitWatcherActor {

}