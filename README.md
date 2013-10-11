NB: To use this plugin, I've to put javax.mail-1.4.1.jar and javax-activation-1.1.jar in the boot directory of SBT.
~/.sbt/boot/scala-2.10.2/org.scala-sbt/sbt/0.13.0

And add in the plugins.sbt of the user 

initialize ~= {
  _ =>
    System.setProperty("com.amazonaws.sdk.disableCertChecking", "true")
}


To import the settings:
AwsPlugin.awsSettings
