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
* awsKeypair : [required] path to the keypair to use

## Tasks

* awsRequest <count=1> : request new VM on EC2
* awsList : list all VM linked to this project
* awsKill <instanceId> : terminate the VM
* awsStart <instanceId> : terminate the VM
* awsStop <instanceId> : terminate the VM
* awsKillAll : terminate all VM linked to this project