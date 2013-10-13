package fr.xebia.sbt.plugin.aws

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model._
import scala.concurrent.{Future, ExecutionContext}

class EC2(client: AmazonEC2Client)(implicit val executionContext: ExecutionContext) {

  def run(request: RunInstancesRequest): Future[RunInstancesResult] = {
    Future {
      client.runInstances(request)
    }
  }

  def run(request: CreateTagsRequest) {
    Future {
      client.createTags(request)
    }
  }

  def run(request: TerminateInstancesRequest): Future[TerminateInstancesResult] = {
    Future {
      client.terminateInstances(request)
    }
  }

  def run(request: StopInstancesRequest): Future[StopInstancesResult] = {
    Future {
      client.stopInstances(request)
    }
  }

  def run(request: StartInstancesRequest): Future[StartInstancesResult] = {
    Future {
      client.startInstances(request)
    }
  }

  def run(request: DescribeInstancesRequest): Future[DescribeInstancesResult] = {
    Future {
      client.describeInstances(request)
    }
  }

}

object EC2 {

  implicit val executionContext = ExecutionContext.Implicits.global

  def apply(endpoint: String): EC2 = {
    val client = new AmazonEC2Client(new EnvironmentVariableCredentialsProvider())
    client.setEndpoint(endpoint)
    new EC2(client)
  }

}