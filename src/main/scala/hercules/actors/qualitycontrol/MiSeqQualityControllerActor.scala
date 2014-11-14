package hercules.actors.qualitycontrol

import hercules.entities.ProcessingUnit

import akka.actor.Props

object MiSeqQualityControllerActor {
  def props(): Props =
    Props(new MiSeqQualityControllerActor())
}

/**
 *
 * TODO: This is not yet implemented.
 *
 * Concrete implementation for doing quality control on a Illumina
 * MiSeq runfolder
 */
class MiSeqQualityControllerActor extends IlluminaQualityControllerActor {

  def passesQualityControl(processingUnit: ProcessingUnit) = ???
  def receive = ???

}