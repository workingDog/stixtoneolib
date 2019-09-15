
organization := "com.github.workingDog"

name := "stixtoneolib"

version := (version in ThisBuild).value

scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "org.neo4j" % "neo4j" % "3.3.9",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.slf4j" % "slf4j-nop" % "1.7.26",
  "com.github.workingDog" %% "scalastix" % "1.1-SNAPSHOT"
)

homepage := Some(url("https://github.com/workingDog/StixToNeoLib"))

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

scalacOptions := Seq("-unchecked", "-deprecation")