package hercules.entities

import java.net.URI

import spray.json.{ JsBoolean, JsString, JsObject, JsValue }

/**
 * A atomic unit to be processed by Hercules. E.g. a Illumina runfolder.
 */
trait ProcessingUnit {

  /**
   * The URI identifying the processing unit
   */
  val uri: URI

  /**
   * The name of the processing unit
   */
  def name: String

  /**
   * Specify if this processing unit has already been found.
   */
  def isFound: Boolean

  /**
   * Values to be mapped to json representation.
   * Override in implementing class to add more information.
   * @return the key value pairs representing the object in json
   */
  protected def mappedValues: Map[String, JsValue] =
    Map(
      "name" -> JsString(name),
      "uri" -> JsString(uri.toString),
      "isFound" -> JsBoolean(isFound)
    )

  /**
   * Convert processing unit to json
   * @return the json representation of the ProcessingUnit
   */
  def toJson: JsValue = {
    JsObject(mappedValues)
  }

}