package fr.xebia.sbt.plugin

import sbt.{Keys => SbtKeys, _}


object AwsPlugin extends Plugin {

  lazy val awsRegion = settingKey[String]("URL Endoit for AWS API. (Default to https://ec2.eu-west-1.amazonaws.com)")
  lazy val awsKeypair = settingKey[String]("Keypair to be associated with the instance (no default).")

  val awsSettings = {
    Seq(
      awsRegion := "https://ec2.eu-west-1.amazonaws.com",
      SbtKeys.commands ++= Seq(
        Request.command,
        ListInstance.command,
        KillAll.command,
        KillInstance.command)
    )
  }

}