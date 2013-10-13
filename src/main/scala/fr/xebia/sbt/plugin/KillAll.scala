package fr.xebia.sbt.plugin

import sbt.Command
import fr.xebia.sbt.plugin.aws.Instance
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import Util.projectName

object KillAll {

  lazy val command = Command.command(
    "awsKillAll",
    "Terminates all instances with tag origin='sbt-plugin' and with a tag value of the project.",
    "Terminates all instances with tag origin='sbt-plugin' and with a tag value of the project.") {
    implicit state => {
      implicit val ec2 = Util.client
      import ec2.executionContext

      Await.ready(
        for {
          instances <- Instance.list(projectName)
          terminations <- Future.sequence(
            for (instance <- instances)
            yield {
              state.log.info(s"AWS: Terminating ${instance.id}")
              instance.terminate
            })
        } yield terminations

        ,
        atMost = 2 minutes)

      state
    }
  }

}
