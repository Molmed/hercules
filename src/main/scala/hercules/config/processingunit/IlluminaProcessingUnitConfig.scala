package hercules.config.processingunit

import java.io.File

import spray.json.{ JsString, JsObject, JsValue }

/**
 * Provides the configuration files for a IlluminaProcessingUnit
 *
 * @param sampleSheet
 * @param QCConfig
 * @param programConfig
 */
case class IlluminaProcessingUnitConfig(
    sampleSheet: File,
    QCConfig: File,
    programConfig: Option[File]) extends ProcessingUnitConfig {

  override def toJson: JsValue = {

    val programConfigFileString =
      if (programConfig.isDefined) programConfig.get.getAbsolutePath
      else "no program config set"

    JsObject("samplesheet" -> JsString(sampleSheet.getAbsolutePath),
      "qcconfig" -> JsString(QCConfig.getAbsolutePath),
      "programconfig" -> JsString(programConfigFileString))

  }
}