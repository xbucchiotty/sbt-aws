package fr.xebia.sbt.plugin

import sbt.Command
import fr.xebia.sbt.plugin.aws.{Instance, EC2}
import scala.concurrent.Await
import scala.concurrent.duration._
import Util.projectName

object KillAll {

  lazy val command = Command.command(
    "awsKillAll",
    "Terminates all instances with tag origin='sbt-plugin' and with a tag value of the project.",
    "Terminates all instances with tag origin='sbt-plugin' and with a tag value of the project.") {
    implicit state => {
      implicit val ec2 = EC2("https://ec2.eu-west-1.amazonaws.com")
      import ec2.executionContext

      state.log.info("AWS: Terminating all instances")

      Await.ready(
        Instance.killAll(projectName),
        atMost = 2 minutes
      )

      state
    }
  }

}
