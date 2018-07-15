name := "AkkaDht22"
organization := "tech.navicore"

version := "0.1.0"

scalaVersion := "2.12.6"
val akkaVersion = "2.5.13"
val circeVersion = "0.9.3"

libraryDependencies ++= Seq(

  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",

  "com.softwaremill.sttp" %% "core" % "1.2.0-RC1",

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,

  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "0.20",

  "org.bidib.com.pi4j" % "pi4j-core" % "1.2.M1",

  "org.scalatest" %% "scalatest" % "3.0.1" % "test"

)

mainClass in assembly := Some("onextent.iot.pijvmpoc2.Main")
assemblyJarName in assembly := "AkkaDht22.jar"

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case PathList("META-INF", _ @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

