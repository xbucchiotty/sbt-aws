import fr.xebia.sbt.plugin.{KillInstance, KillAll, ListInstance, Request}
import sbt._


object AwsPlugin extends Plugin {

  // a group of settings ready to be added to a Project
  // to automatically add them, do
  val awsSettings = Seq(
    Keys.commands ++= Seq(
      Request.command,
      ListInstance.command,
      KillAll.command,
      KillInstance.command)
  )

}