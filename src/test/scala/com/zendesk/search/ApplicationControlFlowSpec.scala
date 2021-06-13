package com.zendesk.search

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

import cats.effect.IO
import com.zendesk.search.RunConsole.SearchSelection
import com.zendesk.search.io.ConsoleIO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.zendesk.search.model.{ Organisation, Ticket, User }
import com.zendesk.search.repo.Repo
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import com.zendesk.search.Search.SearchResult

class ApplicationControlFlowSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with TestSupport
    with ScalaCheckPropertyChecks
    with Arbitraries {
  "Application Control Exit Flow" - {
    "can exit if user gives quit" in {
      RunConsole
        .apply(consoleQuit, onContinue = _ => IO.pure("Continue"), onExit = _ => IO.pure("Exit"))
        .map(str => str shouldBe ("Exit"))
    }
  }

  "Application Control Continue" - {
    "can search, and continue recursion if user gives any string except quit" in {
      val readLineState   = new AtomicReference[String]("")
      val searchCompleted = new AtomicBoolean(false)

      // initial readLine is empty, however, after recursion readLine returns "quit"
      val testConsole =
        ConsoleIO.from(put = _ => IO.unit, read = IO(readLineState.get()) <* IO(readLineState.set("quit")))

      RunConsole
        .apply(testConsole, onContinue = _ => IO(searchCompleted.set(true)).map(_ => "Continue"), onExit = _ => IO.pure("Exit"))
        .map { str =>
          searchCompleted.get shouldBe true
          str shouldBe ("Exit")
        }
    }
  }

  "Application Control Wrong Selection" - {
    "show error message, and continue recursion if user gives wrong selection" in {
      val readLineState    = new AtomicReference[String]("wrongselection")
      val searchCompleted  = new AtomicBoolean(false)
      val errorMessageFlag = new AtomicReference[Option[String]](None)

      /**
       * A console that initially returns a wrong selection,
       * and when invoked again (due to recursion) exits.
       * Search shouldn't be invoked
       */
      val testConsole =
        ConsoleIO.from(
          put = message => IO(errorMessageFlag.set(Some(message))),
          read = IO(readLineState.get()) <* IO(readLineState.set("quit"))
        )

      RunConsole
        .continue(
          emptySearchImpl,
          testConsole
        )
        .map { _ =>
          searchCompleted.get shouldBe (false)
          errorMessageFlag.get().isDefined shouldBe (true)
        }
    }
  }

  "Application Control Flow User Search" - {
    "can search users given the correct search criteria" in {
      forAll { (str: String, users: List[User]) =>
        val testConsole = ConsoleIO.from(_ => IO.unit, IO(str))
        val searchImpl  =
          Search(Repo.from(_ => IO.pure(users.headOption), _ => IO.pure(users)), emptyRepoOrg, emptyRepoTicket)

        val result = RunConsole
          .run(
            SearchSelection.UserSearch,
            searchImpl,
            testConsole
          )
          // FIXME: Latest cats test integration (have found) wrong
          .unsafeRunSync()(cats.effect.unsafe.implicits.global)

        result shouldBe SearchResult.Users(users.map(user => Search.Result(user, Nil, Nil)))

      }
    }
  }

  "Application Control Flow Ticket Search" - {
    "can search tickets given the correct search criteria" in {
      forAll { (str: String, tickets: List[Ticket]) =>
        val testConsole = ConsoleIO.from(_ => IO.unit, IO(str))
        val searchImpl  =
          Search(emptyRepoUser, emptyRepoOrg, Repo.from(_ => IO.pure(tickets.headOption), _ => IO.pure(tickets)))

        val result = RunConsole
          .run(
            SearchSelection.TicketSearch,
            searchImpl,
            testConsole
          )
          // FIXME: Latest cats test integration (have found) wrong
          .unsafeRunSync()(cats.effect.unsafe.implicits.global)

        result shouldBe SearchResult.Tickets(tickets.map(ticket => Search.Result(ticket, Nil, Nil)))
      }
    }
  }

  "Application Control Flow Organisation Search" - {
    "can search organisations given the correct search criteria" in {
      forAll { (str: String, organisations: List[Organisation]) =>
        val testConsole = ConsoleIO.from(_ => IO.unit, IO(str))
        val searchImpl  =
          Search(emptyRepoUser, Repo.from(_ => IO.pure(organisations.headOption), _ => IO.pure(organisations)), emptyRepoTicket)

        val result = RunConsole
          .run(
            SearchSelection.OrganisationSearch,
            searchImpl,
            testConsole
          )
          // FIXME: Latest cats test integration (have found) wrong
          .unsafeRunSync()(cats.effect.unsafe.implicits.global)

        result shouldBe SearchResult.Organisations(organisations.map(ticket => Search.Result(ticket, Nil, Nil)))
      }
    }
  }
}
