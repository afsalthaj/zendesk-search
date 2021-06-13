package com.zendesk.search.config

import com.monovore.decline.Command
import com.monovore.decline.Opts

final case class AppConfig(user: String, quite: Boolean, orgFilePath: String, ticketFilePath: String, userFilePath: String)

import cats.syntax.apply._

object AppConfig {
  val config = Command(name = "ZenDesk Search Application", header = "Welcome to ZenDesk Search!") {

    val orgPath =
      Opts.option[String]("org-path", help = "Path to organization data").withDefault("/Users/afsalthaj/MelbourneCoding/organizations.json")

    val ticketPath =
      Opts
        .option[String]("ticket-path", help = "Path to ticket data")
        .withDefault("/Users/afsalthaj/MelbourneCoding/tickets.json")

    val userPath =
      Opts
        .option[String]("user-file", help = "Path to user data")
        .withDefault("/Users/afsalthaj/MelbourneCoding/users.json")

    val quietOpt = Opts.flag("quiet", help = "Whether to be quiet.").orFalse

    (quietOpt, orgPath, ticketPath, userPath).mapN { (quiet, org, ticket, user) =>
      AppConfig(user, quiet, org, ticket, user)
    }
  }

}
