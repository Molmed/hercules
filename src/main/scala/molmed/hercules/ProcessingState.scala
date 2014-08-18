package molmed.hercules

object ProcessingState extends Enumeration {
  type ProcessingState = Value
  //@TODO Clean up list of possible states
  val Halted, Found, Running, RunningDemultiplexing, FinishedDemultiplexing, RunningAeacusReport, RunningDelivery, VerifiedArchive, Finished = Value
}