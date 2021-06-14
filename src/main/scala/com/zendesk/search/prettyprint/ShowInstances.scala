package com.zendesk.search.prettyprint

import cats.Show
import io.circe.Json
import cats.syntax.show._
import com.zendesk.search.ZenDeskSearch.{ GenericResult, ZenDeskSearchResult }
import com.zendesk.search.ZenDeskSearch.ZenDeskSearchResult.{ Organisations, Tickets, Users }
import com.zendesk.search.model.{ Organisation, Ticket, User }
import com.zendesk.search.repo.Field

/**
 * Pretty-print of every data structure
 * in the app makes use of Show instance.
 * This implies, whenever a data needs to be pretty printed,
 * then do the following:
 *
 * {{{
 *   // Make sure the instance is available here
 *   import cats.syntax.show._
 *   data.show
 * }}}
 * ShowInstances is not available at a package/generic level.
 * This should ideally be used only in `RunConsole`/`Main` part
 * of the application.
 */
trait ShowInstances {
  implicit val showJson: Show[Json] = (t: Json) => {
    t.fold(
      "",
      t => t.toString,
      t => t.toString,
      identity,
      t => t.map(_.show).mkString(", "),
      t => t.show
    )
  }

  implicit val showTicket: Show[Ticket] =
    (t: Ticket) => s"""
                      |
                      |## TICKET
                      |----------------------------------------
                      |${prettyPrintFields(t.fields)}
                      |
                      |----------------------------------------
                      |
                      |""".stripMargin

  implicit val showUser: Show[User] =
    (t: User) => s"""
                    |
                    |## USER
                    |----------------------------------------
                    |${prettyPrintFields(t.fields)}
                    |
                    |----------------------------------------
                    |
                    |""".stripMargin

  implicit val showOrganisation: Show[Organisation] =
    (t: Organisation) => s"""
                            |
                            |## ORGANISATION
                            |----------------------------------------
                            |${prettyPrintFields(t.fields)}
                            |
                            |----------------------------------------
                            |
                            |""".stripMargin

  implicit def showGenericResult[A: Show, B: Show, C: Show]: Show[GenericResult[A, B, C]] = (t: GenericResult[A, B, C]) =>
    List(t.a.show, t.b.map(_.show).mkString("\n"), t.c.map(_.show).mkString("\n")).mkString("\n")

  implicit val showZenDeskSearchResult: Show[ZenDeskSearchResult] = {
    case Users(list)         => lineSeparatedString(list.map(r => r.show))
    case Organisations(list) => lineSeparatedString(list.map(r => r.show))
    case Tickets(list)       => lineSeparatedString(list.map(r => r.show))
  }

  def lineSeparatedString(list: List[String]): String =
    if (list.isEmpty) "-- Not Found --" else list.mkString("\n")

  def prettyPrintFields[K: Show, V: Show](list: List[Field[K, V]]): String =
    if (list.isEmpty)
      ""
    else {
      val maxKeySize = list.map(_.k.show.length).max

      val paddedList =
        list.map { r =>
          s"${r.k.show.padTo(maxKeySize, " ").mkString}  :  ${r.v.show}"
        }

      paddedList.mkString("\n")
    }
}

object ShowInstances extends ShowInstances
