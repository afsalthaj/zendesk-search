import sbt._

object Dependencies {
  object versions {
    val decline       = "1.3.0"
    val monocle       = "2.1.0"
    val fs2           = "3.0.4"
    val circe         = "0.14.0"
    val catsScalaTest = "1.1.1"
    val scalaTestPlus = "3.1.0.0-RC2"
  }

  val decline       = "com.monovore"               %% "decline"                       % versions.decline
  val monocle       = "com.github.julien-truffaut" %% "monocle-core"                  % versions.monocle
  val fs2           = "co.fs2"                     %% "fs2-io"                        % versions.fs2
  val circeFs2      = "io.circe"                   %% "circe-fs2"                     % versions.circe
  val circeCore     = "io.circe"                   %% "circe-parser"                  % versions.circe
  val catsScalaTest = "org.typelevel"              %% "cats-effect-testing-scalatest" % versions.catsScalaTest % Test
  val scalaTestPlus = "org.scalatestplus"          %% "scalatestplus-scalacheck"      % versions.scalaTestPlus % Test
}
