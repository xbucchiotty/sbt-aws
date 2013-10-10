name := "sbt-aws-plugin"

organization := "fr.xebia.sbt.plugin"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.0"

sbtPlugin := true

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk" % "1.5.4" excludeAll (ExclusionRule(name="commons-logging"),ExclusionRule(name="commons-codec")),
  "net.iharder" % "base64" % "2.3.8",
  "javax.mail" % "mail" % "1.4.1",
  "com.google.guava" % "guava" % "14.0.1"
)
