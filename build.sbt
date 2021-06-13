import Dependencies._

name := "zendesk-search"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(decline, monocle, fs2, circeFs2, circeCore, catsScalaTest, scalaTestPlus)

import sbtassembly.AssemblyPlugin.defaultUniversalScript

ThisBuild / assemblyPrependShellScript := Some(defaultUniversalScript(shebang = false))

lazy val app = (project in file("."))
  .settings(
    assembly / assemblyJarName := s"${name.value}"
  )
