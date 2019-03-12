
organization := "com.github.workingDog"

name := "stixtoneolib"

version := (version in ThisBuild).value

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.neo4j" % "neo4j" % "3.3.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha4" % Test,
  "com.github.workingDog" %% "scalastix" % "0.9"
)

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case PathList(xs@_*) if xs.last.toLowerCase endsWith ".dsa" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last.toLowerCase endsWith ".sf" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last.toLowerCase endsWith ".des" => MergeStrategy.discard
  case PathList(xs@_*) if xs.last endsWith "LICENSES.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

homepage := Some(url("https://github.com/workingDog/StixToNeoLib"))

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

assemblyJarName in assembly := "stixtoneolib-" + version.value + ".jar"

scalacOptions := Seq("-unchecked", "-deprecation")