package com.zendesk.search.config

import com.monovore.decline.Command
import com.monovore.decline.Opts

import java.io.File
import java.net.URL
import java.nio.file.Paths

final case class AppConfig(user: String, quite: Boolean, orgFilePath: String, ticketFilePath: String, userFilePath: String)

import cats.syntax.apply._

object AppConfig {
  val config = Command(name = "ZenDesk Search Application", header = "Welcome to ZenDesk Search!") {

    val orgPath =
      Opts
        .option[String]("org-path", help = "Path to organization data")

    val ticketPath =
      Opts
        .option[String]("ticket-path", help = "Path to ticket data")

    val userPath =
      Opts
        .option[String]("user-file", help = "Path to user data")

    val quietOpt = Opts.flag("quiet", help = "Whether to be quiet.").orFalse

    (quietOpt, orgPath, ticketPath, userPath).mapN { (quiet, org, ticket, user) =>
      AppConfig(user, quiet, org, ticket, user)
    }
  }
}
