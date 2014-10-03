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

  val runfolderRoot = new File("test_runfolders")
  val sampleSheetRoot = new File("test_samplesheets")

  val customQCConfigRoot = new File("custom_qc_configs")
  val defaultQCConfigFile = new File("default_qc_config")

  val customProgramConfigRoot = new File("test_custom_program_configs")
  val defaultProgramConfigFile = new File("default_program_config")

  val fetcherConfig = new IlluminaProcessingUnitFetcherConfig(
    runfolderRoot,
    sampleSheetRoot,
    customQCConfigRoot,
    defaultQCConfigFile,
    customProgramConfigRoot,
    defaultProgramConfigFile,
    log)

  val listOfDirsThatNeedSetupAndTeardown =
    List(runfolderRoot,
      sampleSheetRoot,
      customProgramConfigRoot,
      customQCConfigRoot)

  val runfolders = List(
    "140806_D00457_0045_AC47TFACXX",
    "140806_D00457_0046_BC48R0ACXX",
    "140812_M00485_0148_000000000-AA3LB",
    "140821_D00458_0030_BC4F53ANXX",
    "140822_M00485_0150_000000000-A9YTW").
    map(x => new File(runfolderRoot + "/" + x))

  def createMinimalRunParametersXml(runfolder: File) = {
    val runParameters = new File(runfolder + "/RunParameters.xml")

    val controlSoftwareType = if (runfolder.getName().contains("M00485"))
      "MiSeq Control Software"
    else
      "HiSeq Control Software"

    val xmlString =
      s"""<?xml version="1.0"?>
      <RunParameters xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <Setup>
      		<ApplicationName>$controlSoftwareType</ApplicationName>
    	</Setup>
      </RunParameters>      
      """

    val printwriter = new PrintWriter(runParameters)
    printwriter.print(xmlString)
    printwriter.close()
  }

  def createRunFolders(takeXFirst: Int = 2): Seq[File] = {

    defaultQCConfigFile.createNewFile()
    defaultProgramConfigFile.createNewFile()

    val created = runfolders.take(takeXFirst).map(x => {
      x.mkdirs()
      (new File(sampleSheetRoot + "/" + x.getName() + "_samplesheet.csv")).createNewFile()
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
          sampleSheet = new File(sampleSheetRoot + "/" + f.getName() + "_samplesheet.csv"),
          QCConfig = defaultQCConfigFile,
          programConfig = Some(defaultProgramConfigFile))

      if (f.getName().contains("M00485"))
        new MiSeqProcessingUnit(expectedConfig, new URI("file:" + f.getAbsolutePath() + "/"))
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
    val foundFile = new File(pathToFoundFolder + "/found")
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
    new File(runfolders(1) + "/found").createNewFile()

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
    runfolderRoot.listFiles().flatMap(x => x.listFiles()).
      find(p => p.getName() == "RunParameters.xml").
      foreach(f => f.delete())

    intercept[FileNotFoundException] {
      val fetcher = new IlluminaProcessingUnitFetcher()
      val actual = fetcher.checkForReadyProcessingUnits(fetcherConfig)
    }
  }

}