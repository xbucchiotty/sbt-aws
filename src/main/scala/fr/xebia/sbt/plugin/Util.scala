package fr.xebia.sbt.plugin

import sbt.{Keys, Project, State}

object Util {
  def projectName(implicit state: State) =
    Project.extract(state).get(Keys.name)

  def sbtVersion(implicit state: State) =
    Project.extract(state).get(Keys.sbtVersion)
}
