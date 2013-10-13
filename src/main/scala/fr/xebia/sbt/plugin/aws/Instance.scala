package fr.xebia.sbt.plugin.aws

import collection.JavaConversions._
import com.amazonaws.services.ec2.model._
import scala.concurrent.Future
import com.amazonaws.services.ec2.model
import scala.Predef._
import java.io.{StringWriter, InputStreamReader}
import com.google.common.io.CharStreams

case class Instance(underlying: model.Instance) {

  def isRunning: Boolean = InstanceStateName.Running.equals(InstanceStateName.fromValue(status))

  def isTerminated: Boolean = InstanceStateName.Terminated.equals(InstanceStateName.fromValue(status))

  def isStopped: Boolean = InstanceStateName.Stopped.equals(InstanceStateName.fromValue(status))

  def publicDNS: String = underlying.getPublicDnsName

  def status: String = underlying.getState.getName

  def instanceType: String = underlying.getInstanceType

  def id: String = underlying.getInstanceId

  def terminate(implicit ec2: EC2): Future[Instance] = {
    import ec2.executionContext

    val terminationRequest = new TerminateInstancesRequest().withInstanceIds(underlying.getInstanceId)

    for {
      _ <- ec2.run(terminationRequest)
      terminatedInstance <- waitForTermination
    } yield terminatedInstance

  }

  def stop(implicit ec2: EC2): Future[Instance] = {
    import ec2.executionContext

    val stopRequest = new StopInstancesRequest().withInstanceIds(underlying.getInstanceId)

    for {
      _ <- ec2.run(stopRequest)
      stoppedInstance <- waitForStop
    } yield stoppedInstance

  }

  def start(implicit ec2: EC2): Future[Instance] = {
    import ec2.executionContext

    val startRequest = new StartInstancesRequest().withInstanceIds(underlying.getInstanceId)

    for {
      _ <- ec2.run(startRequest)
      startedInstance <- waitForInitialization
    } yield startedInstance

  }

  def refresh(implicit ec2: EC2): Future[Instance] = {
    import ec2.executionContext

    val refreshRequest = new DescribeInstancesRequest().withInstanceIds(id)

    for (response <- ec2.run(refreshRequest))
    yield {
      val refreshedInstance = response.getReservations.flatMap(_.getInstances).head
      this.copy(underlying = refreshedInstance)
    }
  }

  def waitForInitialization(implicit ec2: EC2): Future[Instance] = {
    waitFor(clientHost => clientHost.isRunning)
  }

  def waitForStop(implicit ec2: EC2): Future[Instance] = {
    waitFor(clientHost => clientHost.isStopped)
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

  def apply(instanceId: String)(implicit ec2: EC2): Future[Option[Instance]] = {
    import ec2.executionContext

    ec2.run(new DescribeInstancesRequest().withInstanceIds(List(instanceId)))
      .map(result => result.getReservations.flatMap(_.getInstances).headOption)
      .map(_.map(instance => Instance(instance)))
  }

  def request(name: String, keypair: String, sbtVersion: String, instanceType: String, count: Int = 1)(implicit ec2: EC2): Future[List[Instance]] = {
    import ec2.executionContext

    val creationRequest = new RunInstancesRequest()
      .withInstanceType(InstanceType.fromValue(instanceType))
      .withKeyName(keypair)
      .withMinCount(count)
      .withMaxCount(count)
      .withSecurityGroupIds("accept-all")
      .withUserData(userData(sbtVersion, name))
      .withImageId("ami-c7c0d6b3")

    val nonTerminatedInstances =
      for (response <- ec2.run(creationRequest))
      yield {
        for (ec2Instance <- response.getReservation.getInstances.toList)
        yield Instance(ec2Instance)
      }

    nonTerminatedInstances.onSuccess {
      case instances: List[Instance] => {
        instances.foreach(client => {
          val createTagsRequest = new CreateTagsRequest()
            .withResources(client.id)
            .withTags(
            new Tag("Origin", "sbt-plugin"),
            new Tag("Name", name),
            new Tag("sbt-project", name)
          )
          ec2.run(createTagsRequest)
        })
      }
    }

    nonTerminatedInstances
  }

  def list(name: String)(implicit ec2: EC2): Future[Seq[Instance]] = {
    import ec2.executionContext

    val listingRequest =
      new DescribeInstancesRequest()
        .withFilters(new Filter("tag-value", List("sbt-plugin", name))
      )

    for (listRequest <- ec2.run(listingRequest))
    yield {
      for {
        reservations <- listRequest.getReservations
        instance <- reservations.getInstances
      } yield Instance(instance)

    }.toSeq
  }

  private def userData(sbtVersion: String, projectName: String) =
    CloudInitUserDataBuilder.start
      .addCloudConfig(cloudConfigFile(sbtVersion, projectName))
      .buildBase64UserData

  def cloudConfigFile(sbtVersion: String, projectName: String): String = {

    val cloudConfigAsStream = getClass.getClassLoader.getResourceAsStream("cloudinit/clienthost.txt")
    val cloudConfig = new InputStreamReader(cloudConfigAsStream)

    val sw = new StringWriter()
    CharStreams.copy(cloudConfig, sw)

    sw.toString.format(sbtVersion, projectName)
  }
}