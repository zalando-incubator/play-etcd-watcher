organization := "org.zalando"

name := "etcdwatcher"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  ws,
  specs2 % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.0" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.10" % Test
)

maintainer := "team-payana@zalando.de"
