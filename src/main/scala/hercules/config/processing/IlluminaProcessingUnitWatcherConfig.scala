package hercules.config.processing

/**
 * @param runfolderRootPath
 * @param samplesheetPath
 * @param qcControlConfigPath
 * @param defaultQCConfigFile
 * @param programConfigPath
 * @param defaultProgramConfigFile
 * @param checkForRunfoldersInterval	A interval in seconds to wait 
 * 										between checking for new runfolders
 * 
 * Configure a ProcessingUnit watcher
 */
case class IlluminaProcessingUnitWatcherConfig(
    val runfolderRootPath: String,
    val samplesheetPath: String,
    val qcControlConfigPath: String,
    val defaultQCConfigFile: String,
    val programConfigPath: String,
    val defaultProgramConfigFile: String,
    val checkForRunfoldersInterval: Int) {
  
}