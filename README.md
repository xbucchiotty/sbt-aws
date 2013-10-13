# SBT AWS Plugin

This plugin is a helper to create, list and terminates VM on Amazon EC2 via sbt tasks. Tags are created on the VM
to 'linked' them to this project.

## Prerequisites

You need AWS granted access with variables *AWS_ACCESS_KEY_ID* and *AWS_SECRET_KEY* set as environment variables.

To use this plugin, you'll also need to put javax.mail-1.4.1.jar and javax-activation-1.1.jar in the boot directory of SBT. (~/.sbt/boot/scala-2.10.2/org.scala-sbt/sbt/0.13.0)

Add in the *plugins.sbt*:

    addSbtPlugin("fr.xebia.sbt.plugin" % "sbt-aws-plugin" % "0.0.1-SNAPSHOT")

    initialize ~= {
        _ =>
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true")
    }

Don't forget to import settings with:

    fr.xebia.sbt.plugin.AwsPlugin.awsSettings

## Parameters

* awsEndpoint : URL Endoit for AWS API. (Default to https://ec2.eu-west-1.amazonaws.com)
* awsKeypair : Path to the keypair to be associated with the instance (no default).
* awsInstanceType : Instance type requested (see com.amazonaws.services.ec2.model.InstanceType string description), default to t1.micro

## Tasks

* awsRequest <count=1> : Request a new Instance on EC2.
* awsList : List all requested instances on EC2 with this plugin.
* awsKill <instanceId> : Terminates an instance with the given id.
* awsStart <instanceId> : Start an instance with the given id.
* awsStop <instanceId> : Stop an instance with the given id.
* awsKillAll : Terminates all instances with tag origin='sbt-plugin' and with a tag value of the project.