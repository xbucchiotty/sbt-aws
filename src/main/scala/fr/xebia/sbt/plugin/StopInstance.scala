package fr.xebia.sbt.plugin

import sbt.complete.Parsers._
import sbt.Command
import fr.xebia.sbt.plugin.aws.Instance
import scala.concurrent.Await
import scala.concurrent.duration._

object StopInstance {

  private val arg = Space ~> StringBasic.examples("i-5598fc19")

  lazy val command = Command(
    "awsStop",
    ("id", "Instance of the Id to stop."),
    "Stop an instance with the given id.")(_ => arg) {
    (state, instanceId) => {
      implicit val ec2 = Util.client(state)
      import ec2.executionContext

      state.log.info(s"AWS: Trying to stop instance $instanceId")

      Await.result(
        for (instanceOption <- Instance(instanceId))
        yield instanceOption match {
          case Some(instance) => {
            state.log.info(s"AWS: Stopping $instanceId")
            instance.stop
          }
          case _ => state.log.info(s"AWS: instance $instanceId not found")
        }, atMost = 1 minute
      )

      state
    }
  }
}