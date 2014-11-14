package hercules.protocols

/**
 * Messages which can be used for debuggning, but should not be used
 * in production.
 */
object DebugProtocol {

  /**
   * This should only be used debugging purposes!
   */
  case class StringMessage(s: String)

}