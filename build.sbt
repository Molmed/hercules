name := """hercules"""

version := "1.0"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.3",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test"
)

instrumentSettings

ScoverageKeys.highlighting := true

parallelExecution in Test := false