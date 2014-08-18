package molmed.hercules.sisyphus

import java.io.File

trait SisyphusConfig {

  val currentSisyphusVersion = "v14.1.2"

  //@TODO Make this configurable!
  object BiotankSisyphusConfig {
    val sisyphusInstallLocation: File = new File("/vagrant/sisyphus/")
    val sisyphusLogLocation: File = new File("/vagrant/sisyphus/")
  }

  object UppmaxSisyphusConfig {
    val sisyphusInstallLocation: File = ???
    val sisyphusLogLocation: File = ???
  }

}