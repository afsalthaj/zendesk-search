package com.zendesk.search

import cats.effect.IO
import com.zendesk.search.io.ConsoleIO
import com.zendesk.search.model.{ Organisation, Ticket, User }
import com.zendesk.search.repo.{ Field, Repo }

trait TestSupport {
  def emptyRepo[A]    = Repo.empty[String, Field[String, String], A]
  val emptyRepoUser   = emptyRepo[User]
  val emptyRepoOrg    = emptyRepo[Organisation]
  val emptyRepoTicket = emptyRepo[Ticket]

  val emptySearchImpl = Search(emptyRepoUser, emptyRepoOrg, emptyRepoTicket)

  /**
   * A console that always quits
   */
  val consoleQuit: ConsoleIO =
    ConsoleIO.from(_ => IO.unit, IO.pure("quit"))
}
