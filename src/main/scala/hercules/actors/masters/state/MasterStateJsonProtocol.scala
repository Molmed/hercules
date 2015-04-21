package hercules.actors.masters.state

import spray.json.{ JsValue, RootJsonFormat, DefaultJsonProtocol }

/**
 * Created by johda411 on 2015-04-21.
 */
object MasterStateJsonProtocol extends DefaultJsonProtocol {

  implicit object MessageJsonFormat extends RootJsonFormat[MasterState] {
    override def read(json: JsValue): MasterState = ???
    override def write(obj: MasterState): JsValue = obj.toJson
  }

}
