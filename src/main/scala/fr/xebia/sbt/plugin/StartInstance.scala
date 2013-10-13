package fr.xebia.sbt.plugin

import sbt.complete.Parsers._
import sbt.Command
import fr.xebia.sbt.plugin.aws.Instance
import scala.concurrent.Await
import scala.concurrent.duration._

object StartInstance {

  private val arg = Space ~> StringBasic.examples("i-5598fc19")

  lazy val command = Command(
    "awsStart",
    ("id", "Instance of the Id to start."),
    "Starts an instance with the given id.")(_ => arg) {
    (state, instanceId) => {
      implicit val ec2 = Util.client(state)
      import ec2.executionContext

      state.log.info(s"AWS: Trying to start instance $instanceId")

      Await.result(
        for (instanceOption <- Instance(instanceId))
        yield instanceOption match {
          case Some(instance) => {
            state.log.info(s"AWS: starting $instanceId")
            instance.start
          }
          case _ => state.log.info(s"AWS: instance $instanceId not found")
        }, atMost = 1 minute
      )

      state
    }
  }
}