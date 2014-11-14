package hercules.api

import com.wordnik.swagger.annotations._

import hercules.protocols.HerculesMainProtocol.ProcessingMessage

import scala.annotation.meta.field

/**
 * Models describing the custom types used in the API, with Swagger annotations
 */
package object models {

  /**
   * The MasterState model
   */
  @ApiModel(description = "Master state")
  case class MasterState(
    @(ApiModelProperty @field)(value = "Messages queued for processing") messagesNotYetProcessed: Set[ProcessingMessage],
    @(ApiModelProperty @field)(value = "Messages currently being processed") messagesInProcessing: Set[ProcessingMessage],
    @(ApiModelProperty @field)(value = "Messages that failed during processing") failedMessages: Set[ProcessingMessage])
}