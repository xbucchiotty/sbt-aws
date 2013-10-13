package fr.xebia.sbt.plugin

import sbt.{Keys, Project, State}
import fr.xebia.sbt.plugin.aws.EC2

object Util {
  def projectName(implicit state: State) =
    Project.extract(state).get(Keys.name)

  def sbtVersion(implicit state: State) =
    Project.extract(state).get(Keys.sbtVersion)

  def endpoint(implicit state: State) =
    Project.extract(state).get(AwsPlugin.awsRegion)

  def client(implicit state: State): EC2 =
    EC2(endpoint)

  def keypair(implicit state: State) =
    Project.extract(state).getOpt(AwsPlugin.awsKeypair)
}
