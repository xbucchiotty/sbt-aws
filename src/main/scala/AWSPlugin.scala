import fr.xebia.sbt.plugin.aws.{Instance, EC2}
import sbt._
import scala.concurrent.Await
import scala.util.{Failure, Success}
import concurrent.duration._
import sbt.complete.Parsers._


object AwsPlugin extends Plugin {

  val countArg = (Space ~> IntBasic).?

  private lazy val request = Command("awsRequest", ("count", "Number of instance requested (default 1)."), "Request a new Instance on AWS.")(_ => countArg) {
    (state, arg) => {
      val count = arg.collect {
        case i if i > 0 => i
      }.getOrElse(1)

      state.log.info(s"AWS: Requesting $count instances")

      implicit val ec2 = EC2("https://ec2.eu-west-1.amazonaws.com")
      import ec2.executionContext

      val instancesRequest = Instance.request(projectName(state), count)

      instancesRequest.onComplete(_ match {
        case Success(instances: List[Instance]) =>
          instances.foreach(
            instance => state.log.info(s"AWS: new instance with id ${instance.id}")
          )

        case Failure(e) => state.log.error(e.toString)
      })

      Await.result(instancesRequest, atMost = (30 * count) seconds)

      state
    }
  }

  private lazy val list = Command.command(
    "awsList",
    "List all requested instances on EC2 with this plugin.",
    "List all requested instances on EC2 with this plugin.") {
    state => {
      implicit val ec2 = EC2("https://ec2.eu-west-1.amazonaws.com")
      import ec2.executionContext

      val instancesRequest = Instance.list(projectName(state))

      for (instances <- instancesRequest)
        for ((instance, index) <- instances.zipWithIndex)
        yield state.log.info(s"[$index]\tid:${instance.id}\tstatus:${instance.status}")

      Await.result(instancesRequest, atMost = 2 minutes)

      state
    }
  }

  private lazy val killAll = Command.command(
    "awsKillAll",
    "Terminates all instances with tag origin='sbt-plugin' and with a tag value of the project.",
    "Terminates all instances with tag origin='sbt-plugin' and with a tag value of the project.") {
    state => {
      implicit val ec2 = EC2("https://ec2.eu-west-1.amazonaws.com")
      import ec2.executionContext

      val killRequest = Instance.killAll(projectName(state))

      Await.result(killRequest, atMost = 2 minutes)

      state
    }
  }

  val killArg = (Space ~> IntBasic).?

  // a group of settings ready to be added to a Project
  // to automatically add them, do
  val awsSettings = Seq(
    Keys.commands ++= Seq(request, list, killAll)
  )

  private def projectName(state: State) =
    Project.extract(state).get(Keys.name)

}