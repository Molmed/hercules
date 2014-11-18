package hercules.actors.utils

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import java.net.InetAddress
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach

class MasterLookupTest extends FlatSpec with Matchers {

  object MasterLookupTestImpl extends MasterLookup

  "A MasterLoopup" should " set the current host as the host name in " +
    " the config " in {

      val hostname = InetAddress.getLocalHost().getHostName()
      val actual = MasterLookupTestImpl.getDefaultConfig()

      assert(actual.getString("akka.remote.netty.tcp.hostname") === hostname)
    }

  it should "return actor ref to cluster client receptionist" in {
    val testSystem = ActorSystem("MasterLookupTestSystem")

    val config = ConfigFactory.load()
    val clusterClient =
      MasterLookupTestImpl.getDefaultClusterClient(testSystem, config)

    assert(clusterClient.path.toString() ===
      "akka://MasterLookupTestSystem/user/clusterClient")
  }

  it should "get a cluster client with default configs if nothing else is set" in {

    val testSystem = ActorSystem("MasterLookupTestSystem")

    val clusterClient = MasterLookupTestImpl.getMasterClusterClient(testSystem)

    assert(clusterClient.path.toString() ===
      "akka://MasterLookupTestSystem/user/clusterClient")
  }

}