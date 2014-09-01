package hercules.actors.qualitycontrol

import akka.actor.ActorContext

/**
 * Abstract base class which is to be extended by actors which are to perform
 * quality control on Illumina runfolders
 */
abstract class IlluminaQualityControllerActor extends QualityControllerActor {}