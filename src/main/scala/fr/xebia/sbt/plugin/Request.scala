package fr.xebia.sbt.plugin

import sbt.complete.Parsers._
import sbt.Command
import fr.xebia.sbt.plugin.aws.Instance
import scala.concurrent.Await
import scala.concurrent.duration._
import fr.xebia.sbt.plugin.Util._
import scala.util.Success
import scala.util.Failure

object Request {

  private val countArg = (Space ~> IntBasic).?

  lazy val command = Command(
    "awsRequest",
    ("count", "Number of instance requested (default 1)."),
    "Request a new Instance on EC2.")(_ => countArg)((state, arg) => {

    val count = arg.collect {
      case i if i > 0 => i
    }.getOrElse(1)

    state.log.info(s"AWS: Requesting $count instances")

    implicit val ec2 = Util.client(state)
    import ec2.executionContext

    keypair(state).map(keypair => {
      val instancesRequest = Instance.request(
        projectName(state),
        keypair.getName.split("[.]", 2).head,
        sbtVersion(state),
        instanceType(state),
        count
      )

      instancesRequest.onComplete(_ match {
        case Success(instances: List[Instance]) =>
          instances.foreach(
            instance => state.log.info(s"AWS: new instance with id ${instance.id}")
          )

        case Failure(e) => state.log.error(e.toString)
      })

      Await.result(instancesRequest, atMost = (30 * count) seconds)
    }).getOrElse {
      state.log.error(s"AWS: setting awsKeypair is required.")
    }

    state
  })
}
