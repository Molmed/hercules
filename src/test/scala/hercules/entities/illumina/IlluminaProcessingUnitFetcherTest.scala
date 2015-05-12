package hercules.entities.illumina

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.io.File
import akka.event.Logging
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterEach
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import java.net.URI
import java.io.PrintWriter
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import java.io.FileNotFoundException
import hercules.config.processingunit.IlluminaProcessingUnitFetcherConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig
import hercules.config.processingunit.IlluminaProcessingUnitConfig

class IlluminaProcessingUnitFetcherTest extends FlatSpec with Matchers with BeforeAndAfterEach {

  val system = ActorSystem("testsystem",
    ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]"""))
  val log = system.log

  val runfolderRoots = List(new File("test_runfolders"))
  val sampleSheetRoot = new File("test_samplesheets")

  val customQCConfigRoot = new File("custom_qc_configs")
  val defaultQCConfigFile = new File("default_qc_config")

  val customProgramConfigRoot = new File("test_custom_program_configs")
  val defaultProgramConfigFile = new File("default_program_config")

  val fetcherConfig = new IlluminaProcessingUnitFetcherConfig(
    runfolderRoots,
    sampleSheetRoot,
    customQCConfigRoot,
    defaultQCConfigFile,
    customProgramConfigRoot,
    defaultProgramConfigFile,
    log)

  val listOfDirsThatNeedSetupAndTeardown =
    runfolderRoots ++
      List(
        sampleSheetRoot,
        customProgramConfigRoot,
        customQCConfigRoot)

  val runfolders = List(
    "140806_D00457_0045_AC47TFACXX",
    "140806_D00457_0046_BC48R0ACXX",
    "140812_M00485_0148_000000000-AA3LB",
    "140821_D00458_0030_BC4F53ANXX",
    "140822_M00485_0150_000000000-A9YTW",
    "140924_M00485_0159_000000000-AA5R1").
    map(x => new File(runfolderRoots(0) + "/" + x))

  def createMinimalRunParametersXml(runfolder: File) = {
    val runParameters = new File(runfolder + "/runParameters.xml")
    val runParametersWithEmptyManifest = new File(runfolder + "/runParameters.miseq.empty.xml")
    val runParametersWithManifest = new File(runfolder + "/runParameters.miseq.xml")

    val controlSoftwareType = if (runfolder.getName().contains("M00485"))
      "MiSeq Control Software"
    else
      "HiSeq Control Software"

    val xmlString = runfolder.getName() match {
      case "140822_M00485_0150_000000000-A9YTW" => s"""<?xml version="1.0"?>
        <RunParameters xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <ManifestFiles />
          <Setup>
        		<ApplicationName>$controlSoftwareType</ApplicationName>
      	  </Setup>
        </RunParameters>      
        """
      case "140924_M00485_0159_000000000-AA5R1" => s"""<?xml version="1.0"?>
        <RunParameters xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <ManifestFiles>
      		<string>TruSeq_CAT_Manifest_TC0036120-CAT</string>
      		<string>TruSeq_Custom_Amplicon_Control_Manifest_ACP1.txt</string>
          </ManifestFiles>
          <Setup>
      		<ApplicationName>$controlSoftwareType</ApplicationName>
    	  </Setup>
        </RunParameters>      
        """
      case _ => s"""<?xml version="1.0"?>
      	<RunParameters xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <Setup>
      		<ApplicationName>$controlSoftwareType</ApplicationName>
    	  </Setup>
        </RunParameters>      
        """
    }

    val printwriter = new PrintWriter(runParameters)
    printwriter.print(xmlString)
    printwriter.close()

  }

  def runfolderName2FlowcellName(x: String): String =
    if (x.contains("M00485"))
      x.split("_")(3)
    else
      x.split("_")(3).drop(1) // The first letter (A or B) is not a part of the flowcell name.

  def createRunFolders(takeXFirst: Int = 2): Seq[File] = {

    defaultQCConfigFile.createNewFile()
    defaultProgramConfigFile.createNewFile()

    val created = runfolders.take(takeXFirst).map(x => {
      x.mkdirs()

      val flowCellName = runfolderName2FlowcellName(x.getName)

      (new File(sampleSheetRoot + "/" + flowCellName + "_samplesheet.csv")).createNewFile()
      (new File(x + "/RTAComplete.txt")).createNewFile()
      createMinimalRunParametersXml(x)
      x
    })

    created
  }

  def generateExpectedRunfolders(runfolders: Seq[File], takeXFirst: Int): Seq[IlluminaProcessingUnit] = {

    runfolders.take(takeXFirst).map(f => {

      val expectedConfig =
        new IlluminaProcessingUnitConfig(
          sampleSheet = new File(sampleSheetRoot + "/" + runfolderName2FlowcellName(f.getName()) + "_samplesheet.csv"),
          QCConfig = defaultQCConfigFile,
          programConfig = Some(defaultProgramConfigFile))

      if (f.getName().contains("M00485"))
        new MiSeqProcessingUnit(expectedConfig, new URI("file:" + f.getAbsolutePath() + "/"), false)
      else
        new HiSeqProcessingUnit(expectedConfig, new URI("file:" + f.getAbsolutePath() + "/"))
    })
  }

  def tearDownRunfolders() = {
    runfolders.map(x => FileUtils.deleteDirectory(x))
  }

  override def beforeEach() = {
    listOfDirsThatNeedSetupAndTeardown.map(x => x.mkdirs())
    createRunFolders()
  }

  override def afterEach() = {
    listOfDirsThatNeedSetupAndTeardown.map(x => FileUtils.deleteDirectory(x))
    tearDownRunfolders()
  }

  "A IlluminaProcessingUnitFetcher" should "be able to find new runfolders" in {
    val expected: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(runfolders, 2)

    val fetcher = new IlluminaProcessingUnitFetcher()
    val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)

    assert(expected.toList.sortBy(f => f.uri) === actual.toList.sortBy(f => f.uri))

    for (runfolder <- actual) {
      val uri = runfolder.uri
      val path = new File(uri)
      assert(path.listFiles().exists(p => p.getName() == "found"))
    }

  }

  it should " skip runfolder which have already been found" in {

    val firstCreateTwoRunfolders: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(runfolders, 2)

    val pathToFoundFolder = new File(firstCreateTwoRunfolders(1).uri)
    val foundFile = new File(pathToFoundFolder + "/" + IlluminaProcessingUnit.nameOfIndicatorFile)
    foundFile.createNewFile()

    val expected = firstCreateTwoRunfolders(0)

    val fetcher = new IlluminaProcessingUnitFetcher()
    val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)

    assert(actual.size == 1)
    assert(expected === actual(0))
  }

  it should " disregard runfolders which have already be found" in {

    val expected: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(runfolders, 1)

    // Add a found file to the second runfolder
    // which means that only the first runfolder should be found
    new File(runfolders(1) + "/" + IlluminaProcessingUnit.nameOfIndicatorFile).createNewFile()

    val fetcher = new IlluminaProcessingUnitFetcher()
    val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)

    assert(expected.toList === actual.toList)
  }

  it should " return a MiSeqProcessingUnit for MiSeq runs" in {

    createRunFolders(4)
    val expected: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(runfolders, 4)

    val fetcher = new IlluminaProcessingUnitFetcher()
    val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)

    assert(
      actual.
        filter(x => {
          val asFile = new File(x.uri)
          asFile.getName().contains("M00485")
        }).
        forall(p => p.isInstanceOf[MiSeqProcessingUnit]))
  }

  it should " be able to parse runParameters.xml for ManifestFiles files" in {
    createRunFolders(6)
    val fetcher = new IlluminaProcessingUnitFetcher()
    val actual: Seq[IlluminaProcessingUnit] = fetcher.checkForReadyProcessingUnits(fetcherConfig)

    actual.
      filter(x => {
        val asFile = new File(x.uri)
        asFile.getName().contains("M00485")
      }).foreach(miseq => miseq.name match {
        case "140812_M00485_0148_000000000-AA3LB" =>
          assert(miseq.asInstanceOf[MiSeqProcessingUnit].performeOnMachineAnalysis == false,
            "140812_M00485_0148_000000000-AA3LB should not contain manifest files")
        case "140822_M00485_0150_000000000-A9YTW" =>
          assert(miseq.asInstanceOf[MiSeqProcessingUnit].performeOnMachineAnalysis == false,
            "140822_M00485_0150_000000000-A9YTW should not contain manifest files")
        case "140924_M00485_0159_000000000-AA5R1" =>
          assert(miseq.asInstanceOf[MiSeqProcessingUnit].performeOnMachineAnalysis == true,
            "140924_M00485_0159_000000000 should contain manifest files")
        case _ => assert(false, "Don't know how to evalute this miseq runfolder:" + miseq.name)
      })
  }

  it should " return a HiSeqProcessingUnit for HiSeq runs" in {

    val expected: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(runfolders, 1)

    val fetcher = new IlluminaProcessingUnitFetcher()
    val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)

    assert(
      actual.
        filter(x => {
          val asFile = new File(x.uri)
          !asFile.getName().contains("M00485")
        }).
        forall(p => p.isInstanceOf[HiSeqProcessingUnit]))
  }

  it should " load the custom config files if custom ones are available" in {

    tearDownRunfolders()
    val created = createRunFolders(1)

    // Create custom config files
    val createdRunfolder: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(created, 1)

    val expected = createdRunfolder.map(x => {

      val runfolderName = new File(x.uri).getName()
      val sisyphusFile = new File(customProgramConfigRoot + "/" + runfolderName + "_sisyphus.yml")
      sisyphusFile.createNewFile()
      val qcFile = new File(customQCConfigRoot + "/" + runfolderName + "_qc.xml")
      qcFile.createNewFile()

      val newConfig = x.processingUnitConfig.copy(QCConfig = qcFile, programConfig = Some(sisyphusFile))
      new HiSeqProcessingUnit(newConfig, x.uri)

    })

    val fetcher = new IlluminaProcessingUnitFetcher()
    val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)

    assert(actual === expected, "\n\n" + "actual = " + actual + "\n\n" + "expected=" + expected)
  }

  it should " throw a FileNotFoundException exception if there is no RunParameters.xml file available in the runfolder" in {

    // remove all the RunParameters.xml
    for (runfolder <- runfolderRoots) {
      runfolder.listFiles().flatMap(x => x.listFiles()).
        find(p => p.getName() == "runParameters.xml").
        foreach(f => f.delete())
    }

    intercept[FileNotFoundException] {
      val fetcher = new IlluminaProcessingUnitFetcher()
      val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)
    }
  }

  it should "be able to check if a runfolder is ready or not" in {
    val createdRunfolder: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(runfolders, 2)
    val fetcher = new IlluminaProcessingUnitFetcher()

    // The first one is ready
    assert(fetcher.isReadyForProcessing(createdRunfolder(0)))

    // Mark second one as found, and therefore is not ready
    new File(runfolders(1) + "/" + IlluminaProcessingUnit.nameOfIndicatorFile).createNewFile()
    assert(!fetcher.isReadyForProcessing(createdRunfolder(1)))

  }

  it should "be able to search for runfolders by name" in {

    val createdRunfolder: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(runfolders, 1)
    val fetcher = new IlluminaProcessingUnitFetcher()

    val result = fetcher.searchForProcessingUnitName("140806_D00457_0045_AC47TFACXX", fetcherConfig)

    val expected =
      Some(
        HiSeqProcessingUnit(
          IlluminaProcessingUnitConfig(
            new File("test_samplesheets/C47TFACXX_samplesheet.csv"),
            new File("default_qc_config"),
            Some(new File("default_program_config"))),
          new File("test_runfolders/140806_D00457_0045_AC47TFACXX/").toURI))

    assert(result === expected)

  }

  it should "be able to search for runfolders by name (and find them even if they are marked as found)" in {

    val createdRunfolder: Seq[IlluminaProcessingUnit] = generateExpectedRunfolders(runfolders, 1)
    createdRunfolder.map(_.markAsFound)
    val fetcher = new IlluminaProcessingUnitFetcher()

    val result = fetcher.searchForProcessingUnitName("140806_D00457_0045_AC47TFACXX", fetcherConfig)

    val expected =
      Some(
        HiSeqProcessingUnit(
          IlluminaProcessingUnitConfig(
            new File("test_samplesheets/C47TFACXX_samplesheet.csv"),
            new File("default_qc_config"),
            Some(new File("default_program_config"))),
          new File("test_runfolders/140806_D00457_0045_AC47TFACXX/").toURI))

    assert(result === expected)

  }

  it should "get files (samplesheet, program configs and qc configs) based on the runfolder names first, and if " +
    "no runfolder is available, skip to using the flowcell name" in {

      val runfolderNamedSampleSheet =
        new File(sampleSheetRoot + "/140806_D00457_0045_AC47TFACXX_samplesheet.csv")
      runfolderNamedSampleSheet.createNewFile()

      val runfolderNamedProgramConfig = new File(customProgramConfigRoot + "/140806_D00457_0045_AC47TFACXX_sisyphus.yml")
      runfolderNamedProgramConfig.createNewFile()

      val runfolderNamedQCConfig = new File(customQCConfigRoot + "/140806_D00457_0045_AC47TFACXX_qc.xml")
      runfolderNamedQCConfig.createNewFile()

      val expected =
        List(
          HiSeqProcessingUnit(
            IlluminaProcessingUnitConfig(
              runfolderNamedSampleSheet,
              runfolderNamedQCConfig,
              Some(runfolderNamedProgramConfig)),
            new File("test_runfolders/140806_D00457_0045_AC47TFACXX/").toURI),
          HiSeqProcessingUnit(
            IlluminaProcessingUnitConfig(
              new File(sampleSheetRoot + "/C48R0ACXX_samplesheet.csv"),
              new File("default_qc_config"),
              Some(new File("default_program_config"))),
            new File("test_runfolders/140806_D00457_0046_BC48R0ACXX/").toURI)
        )

      val fetcher = new IlluminaProcessingUnitFetcher()
      val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)

      assert(actual(0) === expected(0))

    }

}