package hercules.actors.qualitycontrol

import akka.actor.ActorContext

/**
 * Base trait which is to be extended by actors which are to perform
 * quality control on Illumina runfolders.
 */
trait IlluminaQualityControllerActor extends QualityControllerActor {}