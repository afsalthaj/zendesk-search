package com.zendesk.search

import cats.Show
import cats.effect.IO
import com.zendesk.search.ZenDeskSearch.{ GenericResult, ZenDeskSearchResult }
import com.zendesk.search.model.Organisation.OrgId
import com.zendesk.search.model.{ FieldNames, Organisation, Ticket, User }
import monocle.{ Lens, Optional }
import com.zendesk.search.repo.{ Field, Repo }
import cats.syntax.traverse._

final case class ZenDeskSearch(
  repoUser: Repo[String, Field[String, String], User],
  repoOrg: Repo[String, Field[String, String], Organisation],
  repoTicket: Repo[String, Field[String, String], Ticket]
) {

  def getAllOrganisations(query: Field[String, String]): IO[ZenDeskSearchResult] =
    get[Organisation, Ticket, User, Field[String, String], String, OrgId](
      repoOrg,
      repoTicket,
      repoUser,
      query,
      oId => Field(FieldNames.ORG_ID, oId.id),
      oId => Field(FieldNames.ORG_ID, oId.id)
    ).map(ZenDeskSearchResult.Organisations)

  def getAllUsers(query: Field[String, String]): IO[ZenDeskSearchResult] =
    get[User, Organisation, Ticket, Field[String, String], String, OrgId](
      repoUser,
      repoOrg,
      repoTicket,
      query,
      oId => Field(FieldNames.PRIMARY_KEY, oId.id),
      oId => Field(FieldNames.ORG_ID, oId.id)
    ).map(ZenDeskSearchResult.Users)

  def getAllTickets(query: Field[String, String]): IO[ZenDeskSearchResult] =
    get[Ticket, Organisation, User, Field[String, String], String, OrgId](
      repoTicket,
      repoOrg,
      repoUser,
      query,
      oId => Field(FieldNames.PRIMARY_KEY, oId.id),
      oId => Field(FieldNames.ORG_ID, oId.id)
    ).map(ZenDeskSearchResult.Tickets)

  /**
   * A/B/C can be Org, Ticket or User
   * Id: PrimaryKey type
   * OrgId: Organization Id type (we could make it configurable if the relationship is based on another field)
   * Q: Query
   */
  def get[A, B, C, Q, Id, OrgId](
    repoA: Repo[Id, Q, A],
    repoB: Repo[Id, Q, B],
    repoC: Repo[Id, Q, C],
    query: Q,
    // FIXME: With a few more techniques this can be made further typesafe,
    // however, we could revisit it if needed.
    searchOrgInB: OrgId => Q,
    searchOrgInC: OrgId => Q
  )(implicit
    optionalOrgId: Optional[A, OrgId],
    primaryKey: Lens[B, Id]
  ): IO[List[GenericResult[A, B, C]]] =
    for {
      as  <- repoA.query(query)
      res <- as.traverse { a =>
               val oId: Option[OrgId] =
                 optionalOrgId.getOption(a)

               for {
                 bs <- oId match {
                         case Some(orgId) => repoB.query(searchOrgInB(orgId))
                         case None        => IO.pure(Nil)
                       }
                 cs <- oId match {
                         case Some(orgId) => repoC.query(searchOrgInC(orgId))
                         case None        => IO.pure(Nil)
                       }
               } yield GenericResult(a, bs, cs)
             }
    } yield res
}

object ZenDeskSearch {
  final case class GenericResult[A, B, C](a: A, b: List[B], c: List[C])

  sealed trait ZenDeskSearchResult

  object ZenDeskSearchResult {
    final case class Users(list: List[GenericResult[User, Organisation, Ticket]])         extends ZenDeskSearchResult
    final case class Organisations(list: List[GenericResult[Organisation, Ticket, User]]) extends ZenDeskSearchResult
    final case class Tickets(list: List[GenericResult[Ticket, Organisation, User]])       extends ZenDeskSearchResult
  }
}
