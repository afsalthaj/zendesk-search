package com.zendesk.search

import cats.Show
import cats.effect.IO
import com.zendesk.search.io.ConsoleIO
import com.zendesk.search.repo.Field
import cats.syntax.show._
import com.zendesk.search.Search.SearchResult

object RunConsole {
  sealed trait FlowSelection

  object FlowSelection {
    def fromString(str: String): FlowSelection =
      str match {
        case "quit" | "q" | "exit" => Exit
        case _                     => Continue
      }

    case object Continue extends FlowSelection
    case object Exit     extends FlowSelection
  }

  abstract sealed class SearchSelection(val int: String)

  object SearchSelection {
    def fromString(str: String): Option[SearchSelection] =
      str match {
        case UserSearch.int         => Some(UserSearch)
        case TicketSearch.int       => Some(TicketSearch)
        case OrganisationSearch.int => Some(OrganisationSearch)
        case _                      => None
      }

    case object UserSearch         extends SearchSelection("1")
    case object TicketSearch       extends SearchSelection("2")
    case object OrganisationSearch extends SearchSelection("3")
  }

  def apply[A](consoleIO: ConsoleIO, onContinue: ConsoleIO => IO[A], onExit: ConsoleIO => IO[A]): IO[A] =
    for {
      _           <- consoleIO.putStrLn("Type 'quit' to exit any time, Press Enter to continue")
      controlFlow <- consoleIO.readLine
      res         <- FlowSelection.fromString(controlFlow) match {
                       case FlowSelection.Continue =>
                         onContinue(consoleIO) *> apply(consoleIO, onContinue, onExit)
                       case FlowSelection.Exit     =>
                         onExit(consoleIO)
                     }
    } yield res

  def exit(consoleIO: ConsoleIO): IO[Unit] =
    consoleIO.putStrLn("Exiting...")

  def continue(searchImpl: Search, consoleIO: ConsoleIO): IO[Unit] =
    for {
      _            <- consoleIO.putStrLn("Select 1 for Users, 2 for Tickets, or 3 for Organizations")
      searchSelStr <- consoleIO.readLine
      _            <- SearchSelection.fromString(searchSelStr) match {
                        case Some(value) =>
                          run(value, searchImpl, consoleIO)
                        case None        =>
                          consoleIO.putStrLn("Wrong selection of search. Select 1, 2 or 3")
                      }
    } yield ()

  def run(
    str: SearchSelection,
    searchImpl: Search,
    consoleIO: ConsoleIO
  ): IO[SearchResult] =
    for {
      lineSep     <- IO(System.lineSeparator())
      _           <- consoleIO.putStrLn("Enter search term")
      searchTerm  <- consoleIO.readLine
      _           <- consoleIO.putStrLn("Enter search value")
      searchValue <- consoleIO.readLine
      query        = Field(searchTerm, searchValue)
      r           <-
        str match {
          case SearchSelection.UserSearch         =>
            runAndShowResult(
              "Searching users...",
              searchImpl.getAllUsers(query),
              lineSep,
              consoleIO
            )
          case SearchSelection.TicketSearch       =>
            runAndShowResult(
              "Searching tickets...",
              searchImpl.getAllTickets(query),
              lineSep,
              consoleIO
            )
          case SearchSelection.OrganisationSearch =>
            runAndShowResult(
              "Searching organisations...",
              searchImpl.getAllOrganisations(Field(searchTerm, searchValue)),
              lineSep,
              consoleIO
            )
        }

    } yield r

  def runAndShowResult[A: Show](searchMessage: String, execute: IO[A], lineSeparator: String, consoleIO: ConsoleIO): IO[A] =
    consoleIO.putStrLn(searchMessage) *>
      execute.flatMap { a =>
        consoleIO.putStrLn(a.show).map(_ => a)
      }
}
