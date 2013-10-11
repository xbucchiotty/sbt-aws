package fr.xebia.sbt.plugin.aws

import collection.JavaConversions._
import com.amazonaws.services.ec2.model.{Filter, DescribeInstancesRequest, TerminateInstancesRequest, InstanceStateName, Tag, CreateTagsRequest, InstanceType, RunInstancesRequest}
import scala.concurrent.Future
import com.amazonaws.services.ec2.model

case class Instance(underlying: model.Instance) {

  def isRunning: Boolean = InstanceStateName.Running.equals(InstanceStateName.fromValue(status))

  def isTerminated: Boolean = InstanceStateName.Terminated.equals(InstanceStateName.fromValue(status))

  def publicDNS: String = underlying.getPublicDnsName

  def status: String = underlying.getState.getName

  def id: String = underlying.getInstanceId

  def terminate(implicit ec2: EC2): Future[Instance] = {
    import ec2.executionContext

    ec2.run(new TerminateInstancesRequest().withInstanceIds(underlying.getInstanceId))
      .flatMap(_ => waitForTermination)
  }

  def refresh(implicit ec2: EC2): Future[Instance] = {
    import ec2.executionContext

    ec2
      .run(new DescribeInstancesRequest().withInstanceIds(id))
      .map(_.getReservations.flatMap(_.getInstances).head)
      .map(newInstanceState => this.copy(underlying = newInstanceState))
  }

  def waitForInitialization(implicit ec2: EC2): Future[Instance] = {
    waitFor(clientHost => clientHost.isRunning)
  }

  def waitForTermination(implicit ec2: EC2): Future[Instance] = {
    waitFor(clientHost => clientHost.isTerminated)
  }

  private def waitFor(predicate: (Instance => Boolean))(implicit ec2: EC2): Future[Instance] = {
    import ec2.executionContext

    refresh.flatMap(result => result match {
      case updated: Instance if predicate(updated) => Future.successful(updated)
      case updated: Instance => {
        updated.waitFor(predicate)
      }
    })
  }
}

object Instance {
  lazy val userData = CloudInitUserDataBuilder.start.addCloudConfigFromFilePath("cloudinit/clienthost.txt").buildBase64UserData

  def apply(instanceId: String)(implicit ec2: EC2): Future[Option[Instance]] = {
    import ec2.executionContext

    ec2.run(new DescribeInstancesRequest().withFilters(new Filter("instanceId", List(instanceId))))
      .map(result => result.getReservations.flatMap(_.getInstances).headOption)
      .map(_.map(instance => Instance(instance)))
  }

  def request(implicit ec2: EC2): Future[Instance] = {
    import ec2.executionContext

    val instanceCreation = ec2.run(
      new RunInstancesRequest()
        .withInstanceType(InstanceType.M1Xlarge)
        .withKeyName("xke-pricer")
        .withMinCount(1)
        .withMaxCount(1)
        .withSecurityGroupIds("accept-all")
        .withUserData(userData)
        .withImageId("ami-c7c0d6b3"))
      .map(result => Instance(result.getReservation.getInstances.head))
      .flatMap(_.waitForInitialization)

    instanceCreation.onSuccess {
      case client: Instance => {
        val createTagsRequest = new CreateTagsRequest()
          .withResources(client.id)
          .withTags(
          new Tag("origin", "sbt-plugin")
        )

        ec2.run(createTagsRequest)
      }
    }

    instanceCreation
  }

  def killAll(implicit ec2: EC2) {
    import ec2.executionContext

    list.map(
      instances => instances.map(_.terminate)
    )
  }

  def list(implicit ec2: EC2): Future[Seq[Instance]] = {
    import ec2.executionContext

    ec2.run(new DescribeInstancesRequest().withFilters(new Filter("tag-value", List("sbt-plugin"))))
      .map(_.getReservations
      .flatMap(_.getInstances)
      .map(Instance(_))
      .toSeq
    )
  }

}