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
    object ProcessingUnitWatcherProtocol {
      sealed trait ProcessingUnitWatcherMessage
      case class FoundProcessingUnit(unit: ProcessingUnit)
    }
}