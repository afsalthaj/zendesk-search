package com.zendesk.search

import cats.Show
import cats.effect.IO
import com.zendesk.search.Search.{ Result, SearchResult }
import com.zendesk.search.model.Organisation.OrgId
import com.zendesk.search.model.{ FIeldNames, Organisation, Ticket, User }
import monocle.{ Lens, Optional }
import com.zendesk.search.repo.{ Field, Repo }
import cats.syntax.show._
import cats.syntax.traverse._

final case class Search(
  repoUser: Repo[String, Field[String, String], User],
  repoOrg: Repo[String, Field[String, String], Organisation],
  repoTicket: Repo[String, Field[String, String], Ticket]
) {

  def getAllOrganisations(query: Field[String, String]): IO[SearchResult] =
    get[Organisation, Ticket, User, Field[String, String], String, OrgId](
      repoOrg,
      repoTicket,
      repoUser,
      query,
      oId => Field(FIeldNames.ORG_ID, oId.id),
      oId => Field(FIeldNames.ORG_ID, oId.id)
    ).map(SearchResult.Organisations)

  def getAllUsers(query: Field[String, String]): IO[SearchResult] =
    get[User, Organisation, Ticket, Field[String, String], String, OrgId](
      repoUser,
      repoOrg,
      repoTicket,
      query,
      oId => Field(FIeldNames.PRIMARY_KEY, oId.id),
      oId => Field(FIeldNames.ORG_ID, oId.id)
    ).map(SearchResult.Users)

  def getAllTickets(query: Field[String, String]): IO[SearchResult] =
    get[Ticket, Organisation, User, Field[String, String], String, OrgId](
      repoTicket,
      repoOrg,
      repoUser,
      query,
      oId => Field(FIeldNames.PRIMARY_KEY, oId.id),
      oId => Field(FIeldNames.ORG_ID, oId.id)
    ).map(SearchResult.Tickets)

  /**
   * A/B/C can be Org, Ticket or User
   * Q: Query
   */
  def get[A: Show, B: Show, C: Show, Q, Id, OrgId](
    repoA: Repo[Id, Q, A],
    repoB: Repo[Id, Q, B],
    repoC: Repo[Id, Q, C],
    query: Q,
    // FIXME: With a little bit more technique this can be made further typesafe,
    // however, we could revisit it if needed.
    searchOrgInB: OrgId => Q,
    searchOrgInC: OrgId => Q
  )(implicit
    Oid: Optional[A, OrgId],
    Cid: Lens[B, Id]
  ): IO[List[Result[A, B, C]]] =
    for {
      as  <- repoA.query(query)
      res <- as.traverse { a =>
               val oId: Option[OrgId] = Oid.getOption(a)

               for {
                 bs <- oId match {
                         case Some(orgId) => repoB.query(searchOrgInB(orgId))
                         case None        => IO.pure(Nil)
                       }
                 cs <- oId match {
                         case Some(orgId) => repoC.query(searchOrgInC(orgId))
                         case None        => IO.pure(Nil)
                       }
               } yield Result(a, bs, cs)
             }
    } yield res
}

object Search {

  sealed trait SearchResult

  object SearchResult {
    implicit val showSearchResult: Show[SearchResult] = {
      case Users(list)         => lineSeparatedString(list.map(r => r.show))
      case Organisations(list) => lineSeparatedString(list.map(r => r.show))
      case Tickets(list)       => lineSeparatedString(list.map(r => r.show))
    }

    def lineSeparatedString(list: List[String]): String =
      if (list.isEmpty) "-- Not Found --" else list.mkString("\n")

    final case class Users(list: List[Result[User, Organisation, Ticket]])         extends SearchResult
    final case class Organisations(list: List[Result[Organisation, Ticket, User]]) extends SearchResult
    final case class Tickets(list: List[Result[Ticket, Organisation, User]])       extends SearchResult
  }

  final case class Result[A, B, C](a: A, b: List[B], c: List[C])

  object Result {
    implicit def showResult[A: Show, B: Show, C: Show]: Show[Result[A, B, C]] = (t: Result[A, B, C]) =>
      List(t.a.show, t.b.map(_.show).mkString("\n"), t.c.map(_.show).mkString("\n")).mkString("\n")
  }
}
