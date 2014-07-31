package molmed.hercules

object ProcessingState extends Enumeration {
  type ProcessingState = Value
  val Halted, Found, Running, Finished = Value
}