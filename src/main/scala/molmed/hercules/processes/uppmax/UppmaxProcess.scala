package molmed.hercules.processes.uppmax

import molmed.hercules.Runfolder
import molmed.hercules.processes.RunfolderProcess
import molmed.hercules.processes.SSHWrappedProcess

trait UppmaxProcess extends SSHWrappedProcess with RunfolderProcess {

  //@TODO Make configurable
  val hostname = "milou-b.uppmax.uu.se"

  //@TODO Might want to investigate if this is a good use case for remote
  // actors. Spawn a remote actor on Uppmax, and then send the jobs through
  // the remote jobrunner.  
  // OR...
  // One might defined two different application entery points and have one
  // actor system start on Biotank and another on Uppmax, and then use
  // the drmaa jobrunner to start things up on uppmax. This is probably the
  // simplest solution.

  //@TODO This should probably be made to run jobs through the drmaa jobrunner
  // A tutorial on how to use the DrmaaJobrunner is available here:
  // http://gridscheduler.sourceforge.net/howto/drmaa_java.html
  override def start(): Runfolder = ???

}