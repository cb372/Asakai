import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Asakai"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.pegdown" % "pegdown" % "1.1.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
