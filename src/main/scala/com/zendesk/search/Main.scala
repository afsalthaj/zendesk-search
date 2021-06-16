package com.zendesk.search

import cats.effect.{ ExitCode, IO }
import cats.syntax.show._
import com.zendesk.search.config.AppConfig
import _root_.io.circe.Json
import io.{ Read, _ }
import cats.syntax.either._
import cats.effect.IOApp
import com.zendesk.search.model.{ Organisation, Ticket, User }
import com.zendesk.search.repo.Repo
import support.{ IOSupport, JsonSupport }

object Main extends IOApp with JsonSupport with IOSupport {
  val consoleIO: ConsoleIO =
    ConsoleIO.live

  override def run(args: List[String]): IO[ExitCode] =
    job(args).attempt
      .flatMap(
        _.fold(
          t => consoleIO.putStrLn(s"Application Failed. ${t}").as(ExitCode.Error),
          _ => consoleIO.putStrLn("Successfully Exited.").as(ExitCode.Success)
        )
      )

  def job(args: List[String]): IO[Unit] =
    for {
      envMap <- IO(sys.env)

      cfg <- IO.fromEither(AppConfig.config.parse(args, envMap).leftMap(help => new RuntimeException(help.show)))

      org <- Repo
               .indexedInMemoryRepo(
                 Read.fromJsonFile(cfg.orgFilePath)(Organisation.fromJson)
               )(_.fields)(_.tokeniseJson)
               .withMessageOnError(s"Failed to read organisation data.")

      user   <- Repo
                  .indexedInMemoryRepo(
                    Read.fromJsonFile(cfg.userFilePath)(User.fromJson)
                  )(_.fields)(_.tokeniseJson)
                  .withMessageOnError(s"Failed to read user data.")
      ticket <- Repo
                  .indexedInMemoryRepo(
                    Read.fromJsonFile(cfg.ticketFilePath)(Ticket.fromJson)
                  )(_.fields)(_.tokeniseJson)
                  .withMessageOnError(s"Failed to read ticket data.")

      searchImpl = ZenDeskSearch(user, org, ticket)
      _         <- RunConsole(consoleIO, RunConsole.continue(searchImpl, _), RunConsole.exit)

    } yield ()
}
