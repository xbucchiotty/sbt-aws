package fr.xebia.sbt.plugin

import sbt.Command
import fr.xebia.sbt.plugin.aws.Instance
import scala.concurrent.Await
import scala.concurrent.duration._
import Util.projectName

object ListInstance {

  lazy val command = Command.command(
    "awsList",
    "List all requested instances on EC2 with this plugin.",
    "List all requested instances on EC2 with this plugin.") {
    implicit state => {
      implicit val ec2 = Util.client
      import ec2.executionContext

      val instancesRequest = Instance.list(projectName)

      for (instances <- instancesRequest) instances match {
        case instances if instances.isEmpty => state.log.info("No instance found")
        case _ =>
          for ((instance, index) <- instances.zipWithIndex)
          yield state.log.info(s"[$index]\t${instance.id}\tstatus:${instance.status}\t${instance.publicDNS}")
      }


      Await.ready(
        instancesRequest,
        atMost = 30 seconds
      )

      state
    }
  }
}
