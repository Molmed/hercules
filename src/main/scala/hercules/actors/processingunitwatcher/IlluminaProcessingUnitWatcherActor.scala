package hercules.actors.processingunitwatcher

import hercules.config.processing.ProcessingUnitWatcherConfig
import akka.actor.Props
import hercules.config.processing.ProcessingUnitWatcherConfig
import hercules.entities.ProcessingUnit
import hercules.actors.HerculesActor

object IlluminaProcessingUnitWatcherActor {
  def props(): Props =
    Props(new IlluminaProcessingUnitWatcherActor())
}

/**
 * Base class for Actors which are watching for finished illumina runfolders.
 */
class IlluminaProcessingUnitWatcherActor()
    extends ProcessingUnitWatcherActor {

  def receive = ???
  def isReadyForProcessing(unit: ProcessingUnit): Boolean = ???
}