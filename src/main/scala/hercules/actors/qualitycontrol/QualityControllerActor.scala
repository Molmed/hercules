package hercules.actors.qualitycontrol

import akka.actor.ActorContext
import hercules.actors.HerculesActor
import hercules.entities.ProcessingUnit

/**
 * Abstract base class for any actor providing quality controll checking
 * functionality
 */
trait QualityControllerActor extends HerculesActor {

  def passesQualityControl(processingUnit: ProcessingUnit)

}