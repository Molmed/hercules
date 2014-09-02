package hercules.actors.processingunitwatcher

import hercules.config.processing.ProcessingUnitWatcherConfig
import akka.actor.Props
import hercules.config.processing.ProcessingUnitWatcherConfig
import hercules.entities.ProcessingUnit

object IlluminaProcessingUnitWatcherActor {
  def props(config: ProcessingUnitWatcherConfig): Props =
    Props(new IlluminaProcessingUnitWatcherActor(config))
}

/**
 * Base class for Actors which are watching for finished illumina runfolders.
 */
class IlluminaProcessingUnitWatcherActor(
  val config: ProcessingUnitWatcherConfig)
    extends ProcessingUnitWatcherActor {

 def receive = ??? 
 def isDone(unit: ProcessingUnit): Boolean = ???
  
}