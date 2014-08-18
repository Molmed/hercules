package molmed.hercules.processes.biotank

import molmed.hercules.Runfolder
import molmed.hercules.sisyphus.SisyphusConfig
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File
import java.nio.file.Paths

class DemultiplexingProcess(val runfolder: Runfolder)
    extends BiotankProcess with SisyphusConfig {

  //@TODO It would be better if the sample sheet was a parameter
  // given to Sisyphus. Right now however, copying will have to work.

  // @TODO Consider if the original sample sheet should be copied to
  // as to be keept.
  val source = Paths.get(runfolder.samplesheet.getPath())
  val sampleSheet = new File(runfolder.runfolder.getAbsolutePath() + "/SampleSheet.csv")
  val targetSamplesheet = Paths.get(sampleSheet.getPath())
  Files.copy(source, targetSamplesheet, StandardCopyOption.REPLACE_EXISTING)

  // Copy the sisyphus yml file.
  val sisyphusConfigFile =
    Paths.get(BiotankSisyphusConfig.sisyphusInstallLocation + "/sisyphus.yml")
  val runfolderSisyphusconfigPath =
    Paths.get(runfolder.runfolder.getAbsolutePath() + "/sisyphus.yml")

  Files.copy(
    sisyphusConfigFile,
    runfolderSisyphusconfigPath,
    StandardCopyOption.REPLACE_EXISTING)

  //@TODO Lose the tee part of the command later. This should only
  // be logged to the file once automated.

  val command =
    BiotankSisyphusConfig.sisyphusInstallLocation +
      "/sisyphus.pl " +
      "-runfolder " + runfolder.runfolder.getAbsolutePath() +
      " -nowait " + 
      " 2>&1 | tee " +
      BiotankSisyphusConfig.sisyphusLogLocation.getAbsolutePath() +
      "/" + runfolder.runfolder.getName() + ".log"

}