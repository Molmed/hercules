package hercules.entities.illumina

import java.io.File
import java.net.URI

import hercules.config.processingunit.IlluminaProcessingUnitConfig
import org.scalatest.{ FlatSpec }
import spray.json.{ JsObject, JsBoolean, JsString, JsValue }

/**
 * Created by johda411 on 2015-04-21.
 */
class IlluminaProcessingUnitTest extends FlatSpec {

  val name = "test"
  val uriToUse = new File(name).toURI
  val found = true

  val progConfig = Some(new File("qc"))
  val qcConfig = new File("qc")
  val sampleSheet = new File("sampleSheet")

  val config = new IlluminaProcessingUnitConfig(sampleSheet, qcConfig, progConfig)

  val processingUnit = new IlluminaProcessingUnit {
    override val uri: URI = uriToUse
    override val processingUnitConfig: IlluminaProcessingUnitConfig = config
    override def isFound: Boolean = found
  }

  "A Illumina processing unit" should "have a json representation" in {

    val expectedJson =
      JsObject(Map(
        "name" -> JsString(name),
        "uri" -> JsString(uriToUse.toString),
        "isFound" -> JsBoolean(found),
        "config" -> config.toJson)
      )

    val asJson = processingUnit.toJson

    assert(asJson.asJsObject === expectedJson)

  }

}
