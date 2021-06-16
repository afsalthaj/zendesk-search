package com.zendesk.search

import cats.effect.IO
import com.zendesk.search.io.ConsoleIO
import com.zendesk.search.model.{ Organisation, Ticket, User }
import com.zendesk.search.repo.{ Field, Repo }
import monocle.Lens

trait TestSupport {
  implicit def lensTuple[A, B]: Lens[(A, B), A] =
    Lens[(A, B), A](_._1)(a => b => (a, b._2))

  def emptyRepo[A]    = Repo.empty[String, Field[String, String], A]
  val emptyRepoUser   = emptyRepo[User]
  val emptyRepoOrg    = emptyRepo[Organisation]
  val emptyRepoTicket = emptyRepo[Ticket]

  val emptySearchImpl = ZenDeskSearch(emptyRepoUser, emptyRepoOrg, emptyRepoTicket)

  /**
   * A console that always quits
   */
  val consoleQuit: ConsoleIO =
    ConsoleIO.from(_ => IO.unit, IO.pure("quit"))

  implicit class RunIO[A](io: IO[A]) {
    def runIO: A = io.unsafeRunSync()(cats.effect.unsafe.implicits.global)
  }
}
