import fr.xebia.sbt.plugin.aws.{Instance, EC2}
import sbt._
import scala.concurrent.Await
import scala.util.{Failure, Success}
import concurrent.duration._


object AwsPlugin extends Plugin {

  private lazy val request = Command.command("awsRequest", "Request a new Instance on AWS.", "Request a new Instance on AWS.") {
    state => {
      state.log.info("AWS: Requesting an instance")

      implicit val ec2 = EC2("https://ec2.eu-west-1.amazonaws.com")
      import ec2.executionContext

      val instanceRequest = Instance.request

      instanceRequest.onComplete(_ match {
        case Success(instance: Instance) =>
          state.log.info(s"AWS: new instance with id ${instance.id}")
        case Failure(e) => state.log.error(e.toString)
      })

      Await.result(instanceRequest, atMost = 2 minutes)

      state
    }
  }

  private lazy val list = Command.command("awsList", "List all requested instances on EC2 with this plugin.", "List all requested instances on EC2 with this plugin.") {
    state => {
      implicit val ec2 = EC2("https://ec2.eu-west-1.amazonaws.com")
      import ec2.executionContext

      val instancesRequest = Instance.list

      for (instances <- instancesRequest) {
        for (instance <- instances)
        yield state.log.info(s"AWS: id:${instance.id}:\tstatus:${instance.status}")
      }

      Await.result(instancesRequest, atMost = 2 minutes)

      state
    }
  }

  // a group of settings ready to be added to a Project
  // to automatically add them, do
  val awsSettings = Seq(
    Keys.commands ++= Seq(request, list)
  )

}