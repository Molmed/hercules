package hercules.protocols

import hercules.protocols.HerculesMainProtocol.HerculesMessage
import org.scalatest.FlatSpec

/**
 * Created by johda411 on 2015-04-21.
 */
class HerculesMainProtocolJsonProtocolTest extends FlatSpec {

  val messageFormat = HerculesMainProtocolJsonProtocol.MessageJsonFormat

  "A HerculesMainProtocolJsonProtocolTest " should "convert messages to Json" in {
    val message: HerculesMessage = new HerculesMainProtocol.Acknowledge()
    assert(messageFormat.write(message).toString() === """{"type":"Acknowledge"}""")
  }

}
