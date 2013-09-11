name := "sbt-aws-plugin"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.0"

sbtPlugin := true

libraryDependencies ++= Seq(
    "com.amazonaws" % "aws-java-sdk" % "1.5.4"
)
