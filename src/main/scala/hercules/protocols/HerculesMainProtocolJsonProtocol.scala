package hercules.protocols

import hercules.protocols.HerculesMainProtocol.HerculesMessage
import spray.json.{ JsValue, RootJsonFormat, DefaultJsonProtocol }

/**
 * Handles conversion from the messages in the HerculesMainProtocol to Json
 * This is used by the API to provide information about state to the
 * outside world.
 *
 * TODO Right now converting from json to class instances is not supported.
 * At the moment we don't need that, but it can be implemented here in the future.
 * /JD 2015-04-21
 *
 * Created by johda411 on 2015-04-21.
 */
object HerculesMainProtocolJsonProtocol extends DefaultJsonProtocol {

  implicit object MessageJsonFormat extends RootJsonFormat[HerculesMessage] {
    override def read(json: JsValue): HerculesMessage = ???
    override def write(obj: HerculesMessage): JsValue = obj.toJson
  }

}
