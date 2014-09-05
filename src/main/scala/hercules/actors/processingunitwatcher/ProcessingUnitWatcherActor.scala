package hercules.actors.processingunitwatcher

import akka.actor.ActorContext
import hercules.actors.HerculesActor
import hercules.config.processing.ProcessingUnitWatcherConfig
import hercules.entities.ProcessingUnit

/**
 * This class will watch for new runfolders and return send them of to
 * the master once they are ready to start processing.
 */
trait ProcessingUnitWatcherActor extends HerculesActor {
  
  val config: ProcessingUnitWatcherConfig
  
  /**
   * Indicate if the unit is ready to be processed.
   * Normally this involves checking files on the file system or reading it's 
   * status from a database.
   * 
   * @param unit The processing unit check
   * @return if the processing unit is ready to be processed or not.
   */
  def isReadyForProcessing(unit: ProcessingUnit): Boolean
  
}