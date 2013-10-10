package fr.xebia.sbt.plugin

import sbt._, Keys._, Project.Initialize
import scala.reflect.io.Path
import fr.xebia.sbt.plugin.aws.EC2

object AWSPlugin extends Plugin {

  import AWS._

  object AWS {
    val awsEndpoint = SettingKey[String]("aws-endpoint", "The URL of the AWS endpoint depending on the requested region (ex: https://eu-west-1.ec2.amazonaws.com for EUROPE).")
    val keypairPath = SettingKey[Option[Path]]("aws-keypair", "The path to the AWS keypair.")
    val ec2 = SettingKey[EC2]("ec2")

    val request = TaskKey[String]("aws-request", "Request a new instance on EC2")
  }

  val awsSettings: Seq[Setting[_]] = Seq(
    awsEndpoint := "https://eu-west-1.ec2.amazonaws.com",
    keypairPath := None,
    ec2 <<= awsEndpoint(EC2.apply),
    request <<= requestTask
  )

  def requestTask = (ec2, streams) map ((ec2, s) => {
    println("good with " + ec2.toString)
    "coucou"
  })


}