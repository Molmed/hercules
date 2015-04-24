package hercules.config.processingunit

import spray.json.JsValue

/**
 * Base class for configuring a processing unit
 */
abstract class ProcessingUnitConfig() {

  def toJson: JsValue = ???

}