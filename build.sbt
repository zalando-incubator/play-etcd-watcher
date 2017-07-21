organization := "org.zalando"

name := "play-etcd-watcher"

scalaVersion := "2.12.2"
crossScalaVersions := Seq("2.11.8", "2.12.2")

libraryDependencies ++= Seq(
  ws,
  guice,
  specs2 % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.6.0" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.12" % Test
)

maintainer := "team-payana@zalando.de"

lazy val `play-etcd-watcher` = (project in file(".")).enablePlugins(PlayScala)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

//pom extra info
publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

scapegoatVersion := "1.3.1"

pomExtra := (
  <scm>
    <url>git@github.com:zalando-incubator/play-etcd-watcher.git</url>
    <developerConnection>scm:git:git@github.com:zalando-incubator/play-etcd-watcher.git</developerConnection>
    <connection>scm:git:https://github.com/zalando-incubator/play-etcd-watcher.git</connection>
  </scm>
  <developers>
    <developer>
      <name>Andrei Kaigorodov</name>
      <email>andrei.kaigorodov@zalando.de</email>
    </developer>
    <developer>
      <name>Oleksandr Volynets</name>
      <email>oleksandr.volynets@zalando.de</email>
      <url>https://github.com/ovolynets</url>
    </developer>
  </developers>
)

homepage := Some(url("https://github.com/zalando-incubator/play-etcd-watcher"))
