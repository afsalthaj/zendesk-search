package com.zendesk.search

import cats.effect.{ ExitCode, IO }
import cats.instances.list._
import cats.syntax.show._
import cats.syntax.traverse._
import com.zendesk.search.config.AppConfig
import _root_.io.circe.Json
import io.{ Read, _ }
import com.zendesk.search.model.{ Organisation, Ticket, User }
import com.zendesk.search.repo.Repo
import support.JsonOps

object ZenDeskSearch extends cats.effect.IOApp with JsonOps {
  override def run(args: List[String]): IO[ExitCode] =
    job(args).as(ExitCode.Success)

  val consoleIO =
    ConsoleIO.live

  def job(args: List[String]) =
    for {
      envMap <- IO(sys.env)

      r <- AppConfig.config.parse(args, envMap) match {
             case Left(help) =>
               help.errors
                 .traverse(consoleIO.putStrLn)
                 .flatMap(_ => consoleIO.putStrLn(help.show))
                 .as(ExitCode.Error)

             case Right(cfg) =>
               for {
                 org <- Repo
                          .indexedInMemoryRepo[Organisation, String, Json, String, String](
                            Read.fromJsonFile(cfg.orgFilePath)(Organisation.fromJson),
                            _.fields,
                            _.decomposeString
                          )
                          .onError(r => consoleIO.putStrLn(s"Failed to read organisation. ${r}"))

                 user   <- Repo
                             .indexedInMemoryRepo[User, String, Json, String, String](
                               Read.fromJsonFile(cfg.userFilePath)(User.fromJson),
                               _.fields,
                               _.decomposeString
                             )
                             .onError(r => consoleIO.putStrLn(s"Failed to read user. ${r}"))
                 ticket <- Repo
                             .indexedInMemoryRepo[Ticket, String, Json, String, String](
                               Read.fromJsonFile(cfg.ticketFilePath)(Ticket.fromJson),
                               _.fields,
                               _.decomposeString
                             )
                             .onError(r => consoleIO.putStrLn(s"Failed to read ticket. ${r}"))

                 searchImpl = Search(user, org, ticket)
                 _         <- RunConsole(consoleIO, RunConsole.continue(searchImpl, _), RunConsole.exit)
               } yield ()
           }
    } yield r

}
