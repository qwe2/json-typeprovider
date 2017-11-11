name := "Json TypeProvider"
organization := "com.example"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.4"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val json4sVer = "3.5.3"

lazy val macros = (project in file("macro"))
  .settings(
    unmanagedClasspath in Compile ++= (unmanagedResources in Compile).value,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
//      for debugging in IDEA
//      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.json4s" %% "json4s-native" % json4sVer,
      "org.json4s" %% "json4s-jackson" % json4sVer
    )
  )

lazy val core = (project in file("core"))
  .dependsOn(macros)
  .settings(
    scalacOptions in ThisBuild ++= Seq(
      "-unchecked",
      "-deprecation",
//      "-Ymacro-debug-lite"
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.3" % "test"
    )
  )
