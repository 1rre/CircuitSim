//This file defines the parameters for the SBT compiler

scalaVersion := "2.13.2"
ThisBuild / turbo := true
libraryDependencies += ("org.scalafx" %% "scalafx" % "14-R19")
Compile / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}
lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map( m =>
  "org.openjfx" % s"javafx-$m" % "14.0.1" classifier osName
)
excludeFilter in unmanagedSources := "circuit*"
enablePlugins(JavaFxPlugin)

javaFxMainClass := "resultsViewer"

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "." + artifact.extension
}
