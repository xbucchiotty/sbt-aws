package fr.xebia.sbt.plugin

import sbt.complete.Parsers._
import sbt.Command
import fr.xebia.sbt.plugin.aws.{Instance, EC2}
import scala.concurrent.Await
import scala.concurrent.duration._
import Util.endpoint

object KillInstance {

  private val killArg = Space ~> StringBasic.examples("i-5598fc19")

  lazy val command = Command(
    "awsKill",
    ("id", "Instance of the Id to kill."),
    "Terminates an instance with the given id.")(_ => killArg) {
    (state, instanceId) => {
      implicit val ec2 = EC2(endpoint(state))
      import ec2.executionContext

      state.log.info(s"AWS: Trying to terminate instance $instanceId")

      Await.result(
        for (instanceOption <- Instance(instanceId))
        yield instanceOption match {
          case Some(instance) => {
            state.log.info(s"AWS: Terminating $instanceId")
            instance.terminate
          }
          case _ => state.log.info(s"AWS: instance $instanceId not found")
        }, atMost = 1 minute
      )

      state
    }
  }
}