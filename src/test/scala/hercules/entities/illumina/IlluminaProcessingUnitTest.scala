package hercules.entities.illumina

import java.io.File
import java.net.URI

import hercules.config.processingunit.IlluminaProcessingUnitConfig
import org.scalatest.{ FlatSpec }

/**
 * Created by johda411 on 2015-04-21.
 */
class IlluminaProcessingUnitTest extends FlatSpec {

  val progConfig = Some(new File("qc"))
  val qcConfig = new File("qc")
  val sampleSheet = new File("sampleSheet")

  val config = new IlluminaProcessingUnitConfig(sampleSheet, qcConfig, progConfig)

  val processingUnit = new IlluminaProcessingUnit {
    override val uri: URI = new File("test").toURI
    override val processingUnitConfig: IlluminaProcessingUnitConfig = config
  }

  "A Illumina processing unit" should "have a json representation" in {

    val expectedJson =
      """{"name":"test","uri":"file:/home/MOLMED/johda411/workspace/hercules/test","isFound":false,"config":{"samplesheet":"/home/MOLMED/johda411/workspace/hercules/sampleSheet","qcconfig":"/home/MOLMED/johda411/workspace/hercules/qc","programconfig":"/home/MOLMED/johda411/workspace/hercules/qc"}}"""

    assert(processingUnit.toJson.toString() === expectedJson.toString)

  }

}
