package com.zendesk.search

import cats.Show
import cats.effect.IO
import com.zendesk.search.io.ConsoleIO
import com.zendesk.search.repo.Field
import cats.syntax.show._
import com.zendesk.search.ZenDeskSearch.ZenDeskSearchResult

object RunConsole {
  def apply[A](
    consoleIO: ConsoleIO,
    onContinue: ConsoleIO => IO[A],
    onExit: ConsoleIO => IO[A]
  ): IO[A] =
    for {
      _           <- consoleIO.putStrLn("Type 'quit' to exit any time, Press Enter to continue")
      controlFlow <- consoleIO.readLine
      res         <- ControlFlow.fromString(controlFlow) match {
                       case ControlFlow.Continue =>
                         onContinue(consoleIO) *> apply(consoleIO, onContinue, onExit)
                       case ControlFlow.Exit     =>
                         onExit(consoleIO)
                     }
    } yield res

  def exit(consoleIO: ConsoleIO): IO[Unit] =
    consoleIO.putStrLn("Exiting...")

  def continue(searchImpl: ZenDeskSearch, consoleIO: ConsoleIO): IO[Unit] =
    for {
      _            <- consoleIO.putStrLn("Select 1 for Users, 2 for Tickets, or 3 for Organizations")
      searchSelStr <- consoleIO.readLine
      _            <- SearchSelection.fromString(searchSelStr) match {
                        case Some(value) =>
                          run(value, searchImpl, consoleIO)
                        case None        =>
                          consoleIO.putStrLn(s"Wrong selection of search. ${searchSelStr} is not supported. Select 1, 2 or 3")
                      }
    } yield ()

  def run(
    searchSelection: SearchSelection,
    searchImpl: ZenDeskSearch,
    consoleIO: ConsoleIO
  ): IO[ZenDeskSearchResult] =
    for {
      lineSep     <- IO(System.lineSeparator())
      _           <- consoleIO.putStrLn("Enter search term")
      searchTerm  <- consoleIO.readLine
      _           <- consoleIO.putStrLn("Enter search value")
      searchValue <- consoleIO.readLine
      query        = Field(searchTerm, searchValue)
      result      <-
        searchSelection match {
          case SearchSelection.UserSearch         =>
            runAndShowResult(
              searchMessage = "Searching users...",
              execute = searchImpl.getAllUsers(query),
              lineSeparator = lineSep,
              consoleIO = consoleIO
            )
          case SearchSelection.TicketSearch       =>
            runAndShowResult(
              searchMessage = "Searching tickets...",
              execute = searchImpl.getAllTickets(query),
              lineSeparator = lineSep,
              consoleIO = consoleIO
            )
          case SearchSelection.OrganisationSearch =>
            runAndShowResult(
              searchMessage = "Searching organisations...",
              execute = searchImpl.getAllOrganisations(Field(searchTerm, searchValue)),
              lineSeparator = lineSep,
              consoleIO = consoleIO
            )
        }

    } yield result

  def runAndShowResult[A: Show](
    searchMessage: String,
    execute: IO[A],
    lineSeparator: String,
    consoleIO: ConsoleIO
  ): IO[A] =
    consoleIO.putStrLn(searchMessage) *>
      execute.flatMap { a =>
        consoleIO.putStrLn(a.show).as(a)
      }

  sealed trait ControlFlow

  object ControlFlow {
    def fromString(str: String): ControlFlow =
      str match {
        case "quit" | "q" | "exit" => Exit
        case _                     => Continue
      }

    case object Continue extends ControlFlow
    case object Exit     extends ControlFlow
  }

  abstract sealed class SearchSelection(val str: String)

  object SearchSelection {
    def fromString(str: String): Option[SearchSelection] =
      str match {
        case UserSearch.str         => Some(UserSearch)
        case TicketSearch.str       => Some(TicketSearch)
        case OrganisationSearch.str => Some(OrganisationSearch)
        case _                      => None
      }

    case object UserSearch         extends SearchSelection("1")
    case object TicketSearch       extends SearchSelection("2")
    case object OrganisationSearch extends SearchSelection("3")
  }
}
