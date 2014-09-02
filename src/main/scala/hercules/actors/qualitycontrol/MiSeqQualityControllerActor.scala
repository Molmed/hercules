package hercules.actors.qualitycontrol

import hercules.entities.ProcessingUnit


/**
 * Concrete implementation for doing quality control on a Illumina
 * MiSeq runfolder
 */
class MiSeqQualityControllerActor extends IlluminaQualityControllerActor {
  
  def passesQualityControl(processingUnit: ProcessingUnit) = ???
  def receive = ???

}