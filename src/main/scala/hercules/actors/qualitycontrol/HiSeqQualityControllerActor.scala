package hercules.actors.qualitycontrol

import hercules.entities.ProcessingUnit

/**
 * Concrete implementation for doing quality control on a Illumina
 * HiSeq runfolder
 */
class HiSeqQualityControllerActor extends IlluminaQualityControllerActor {
 
  def passesQualityControl(processingUnit: ProcessingUnit) = ???
  def receive = ???

}