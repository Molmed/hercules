package hercules

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.slf4j.Logger
import hercules.HerculesStartRoles._

class HerculesEntryPointTest extends FlatSpec with Matchers {

  object HerculesEntryPointTestImpl extends HerculesEntryPoint

  "A HerculesEntryPoint" should "have a logger" in {

    HerculesEntryPointTestImpl.log shouldBe a[Logger]

  }

  it should "translate strings to roles correctly" in {

    HerculesEntryPointTestImpl.string2Role("master") shouldBe a[RunMaster]
    HerculesEntryPointTestImpl.string2Role("demultiplexer") shouldBe a[RunDemultiplexer]
    HerculesEntryPointTestImpl.string2Role("watcher") shouldBe a[RunRunfolderWatcher]
    HerculesEntryPointTestImpl.string2Role("restapi") shouldBe a[RestApi]

  }

  it should "load a correct default config" in {

    val config = new CommandLineOptions(None)
    val actual =
      HerculesEntryPointTestImpl.loadDefaultCommandLineConfig(config)

    assert(actual.applicationType == Some(List(RunMaster(), RestApi())))

  }

  it should "parse the command line options given and start accordingly" in {
    val config = new CommandLineOptions(Some(List(RunHelp())))
    HerculesEntryPointTestImpl.parseCommandLineOptions(config)
  }

}