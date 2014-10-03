import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._
import NativePackagerHelper._
import com.typesafe.sbt.packager.archetypes.ServerLoader
import scalariform.formatter.preferences._

name := """hercules"""

version := "0.1"

scalaVersion := "2.10.2"

libraryDependencies ++= {
  Seq(
  "com.typesafe.akka" %% "akka-contrib" % "2.3.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.4",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.4",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  "com.github.scopt" %% "scopt" % "3.2.0",
  "commons-io" % "commons-io" % "2.4",
  "me.lessis" %% "courier" % "0.1.3",
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "io.spray" %% "spray-can" % "1.3.1",
  "io.spray" %% "spray-routing" % "1.3.1",
  "io.spray" %% "spray-json" % "1.3.0")
}

instrumentSettings

ScoverageKeys.highlighting := true

parallelExecution in Test := false

parallelExecution in ScoverageTest := false

resolvers += Resolver.sonatypeRepo("public")

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

// This is needed for the persistence to work when running from sbt
fork := true

// -----------------------
// Use the Scalariform plugin to make sure that our code is always
// formatted the same way
// -----------------------

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignParameters, true)


// -----------------------
// Stuff for the packager
// -----------------------

packageArchetype.java_server

// Make sure that the application.conf file is loaded from the system
// and not the class path
//bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/application.conf""""

mappings in Universal += { file("src/main/resources/application.conf") -> "conf/application.conf" }

val packageMaintainer = "Johan Dahlberg <johan.dahlberg@medsci.uu.se>"

val packageDescriptionText = "Hercules is a distributed system for processing illumina data." 

maintainer in Linux := packageMaintainer

packageSummary in Linux := packageDescriptionText

serverLoading in Debian := ServerLoader.SystemV

packageDescription := packageDescriptionText

rpmVendor := "ngi-uu"

rpmLicense := Some("MIT")

daemonUser in Linux := "hercules" // user which will execute the application

daemonGroup in Linux := "hercules"    // group which will execute the application

maintainer in Docker := packageMaintainer

