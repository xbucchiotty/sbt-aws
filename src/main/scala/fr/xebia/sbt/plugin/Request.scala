package fr.xebia.sbt.plugin

import sbt.complete.Parsers._
import sbt.Command
import fr.xebia.sbt.plugin.aws.{Instance, EC2}
import scala.util.{Failure, Success}
import scala.concurrent.Await
import scala.concurrent.duration._
import Util.{projectName, sbtVersion}

object Request {

  private val countArg = (Space ~> IntBasic).?

  lazy val command = Command(
    "awsRequest",
    ("count", "Number of instance requested (default 1)."),
    "Request a new Instance on AWS.")(_ => countArg)((state, arg) => {

    val count = arg.collect {
      case i if i > 0 => i
    }.getOrElse(1)

    state.log.info(s"AWS: Requesting $count instances")

    implicit val ec2 = EC2("https://ec2.eu-west-1.amazonaws.com")
    import ec2.executionContext

    val instancesRequest = Instance.request(
      projectName(state),
      count,
      sbtVersion(state)
    )

    instancesRequest.onComplete(_ match {
      case Success(instances: List[Instance]) =>
        instances.foreach(
          instance => state.log.info(s"AWS: new instance with id ${instance.id}")
        )

      case Failure(e) => state.log.error(e.toString)
    })

    Await.result(instancesRequest, atMost = (30 * count) seconds)

    state
  })
}
