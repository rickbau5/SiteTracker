import sbt.project      // temporary bug in IntelliJ not resolving `project in`

scalaVersion := "2.11.7"

lazy val deps = Seq(
  "com.typesafe.akka" % "akka-remote_2.11" % "2.4.1",
  "com.github.nscala-time" %% "nscala-time" % "2.6.0",
  "com.jsuereth" %% "scala-arm" % "1.4"
)

lazy val commonSettings = Seq(
  organization := "com.bau5",
  version := "0.1.0",
  scalaVersion := "2.11.7"
)

lazy val client = (project in file("client")).
  dependsOn(common).
  settings(commonSettings: _*).
  settings(
    name := "SiteTrackerClient",
    mainClass := Some("com.bau5.sitetracker.client.Main")
  )

lazy val server = (project in file("server")).
  dependsOn(common).
  settings(commonSettings: _*).
  settings(
    name := "SiteTrackerServer",
    mainClass := Some("com.bau5.sitetracker.server.Main")
  )

lazy val common = (project in file("common")).
  settings(commonSettings: _*).
  settings(
    name := "SiteTrackerCommon"
  )

libraryDependencies in common ++= deps