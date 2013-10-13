package fr.xebia.sbt.plugin

import sbt._


object AwsPlugin extends Plugin {

  lazy val awsRegion = settingKey[String]("URL Endoit for AWS API. (Default to https://ec2.eu-west-1.amazonaws.com)")
  lazy val awsKeypair = settingKey[File]("Path to the keypair to be associated with the instance (no default).")

  val awsSettings = {
    Seq(
      awsRegion := "https://ec2.eu-west-1.amazonaws.com",
      Keys.commands ++= Seq(
        Request.command,
        ListInstance.command,
        KillAll.command,
        KillInstance.command,
        StartInstance.command,
        StopInstance.command)
    )
  }

}