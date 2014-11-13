package hercules.actors.qualitycontrol

import hercules.entities.ProcessingUnit
import akka.actor.Props

object HiSeqQualityControllerActor {
  def props(): Props =
    Props(new HiSeqQualityControllerActor())
}

/**
 * TODO: This is not yet implemented.
 * 
 * Concrete implementation for doing quality control on a Illumina
 * HiSeq runfolder
 */
class HiSeqQualityControllerActor extends IlluminaQualityControllerActor {

  def passesQualityControl(processingUnit: ProcessingUnit) = ???
  def receive = ???

}