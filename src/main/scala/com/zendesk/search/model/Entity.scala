package com.zendesk.search.model

import cats.Show
import cats.instances.string._
import io.circe.{ Decoder, Json }
import cats.syntax.either._
import monocle.Optional
import monocle.Lens
import com.zendesk.search.repo.Field
import com.zendesk.search.support.{ FieldsPrettyPrint, JsonShowInstance }

/**
 * Note: The entities could actually possess the correct
 * type of fields and values, however, we are intentionally relaxing
 * a bit here by making use of List[Field[String, Json]]
 * as the use-case doesn't necessarily really demand the types to be intact
 * other than primaryKey(possibly) and organization_ids.
 *
 * On json decoding:
 * An alternate is custom derivation for `List[Field[String, Json]]`
 * * and auto derive types using circe-autoderivation. However, given there are only 3 entities,
 * its good to make things explicit as much as we can.
 *
 * Why id for each entity is String? (Could be a newtype as well)
 * sample data has records, where id that can be int or string - so we had to relax it as this may happen in other entities too.
 */
final case class Ticket(fields: List[Field[String, Json]], id: String, orgId: Option[Organisation.OrgId])

object Ticket extends JsonShowInstance {
  def fromJson(json: Json): Either[String, Ticket] = {
    val doc = json.hcursor

    for {
      id <- doc
              .downField(FieldNames.PRIMARY_KEY)
              .as[String](Decoder[String].or(Decoder[Int].emap(r => Right(r.toString))))
              .leftMap(_.message)

      orgId <- doc
                 .downField(FieldNames.ORG_ID)
                 .as[Option[String]](Decoder[Option[String]].or(Decoder[Option[Int]].emap(r => Right(r.map(_.toString)))))
                 .leftMap(_.message)

    } yield Ticket(Field.fromJson(json), id, orgId.map(Organisation.OrgId))
  }

  implicit val ticketId: Lens[Ticket, String] =
    Lens[Ticket, String](_.id)(int => ticket => ticket.copy(id = int))

  implicit val ticketOrgId: Optional[Ticket, Organisation.OrgId] =
    Optional[Ticket, Organisation.OrgId](_.orgId)(a => s => s.copy(orgId = Some(a)))

  implicit val showTicket: Show[Ticket] =
    (t: Ticket) => s"""
                      |
                      |## TICKET
                      |----------------------------------------
                      |${FieldsPrettyPrint(t.fields)}
                      |
                      |----------------------------------------
                      |
                      |""".stripMargin
}

final case class User(fields: List[Field[String, Json]], id: String, orgId: Option[Organisation.OrgId])

object User {
  def fromJson(json: Json): Either[String, User] = {
    val doc = json.hcursor

    for {
      id <- doc
              .downField(FieldNames.PRIMARY_KEY)
              .as[String](Decoder[String].or(Decoder[Int].emap(r => Right(r.toString))))
              .leftMap(_.message)

      orgId <- doc
                 .downField(FieldNames.ORG_ID)
                 .as[Option[String]](Decoder[Option[String]].or(Decoder[Option[Int]].emap(r => Right(r.map(_.toString)))))
                 .leftMap(_.message)

    } yield User(Field.fromJson(json), id, orgId.map(Organisation.OrgId))
  }

  implicit val userId: Lens[User, String] =
    Lens[User, String](_.id)(int => user => user.copy(id = int))

  implicit val userOrgId: Optional[User, Organisation.OrgId] =
    Optional[User, Organisation.OrgId](_.orgId)(a => s => s.copy(orgId = Some(a)))

  implicit val showUser: Show[User] =
    (t: User) => s"""
                    |
                    |## USER
                    |----------------------------------------
                    |${FieldsPrettyPrint(t.fields)}
                    |
                    |----------------------------------------
                    |
                    |""".stripMargin
}

final case class Organisation(fields: List[Field[String, Json]], id: Organisation.OrgId)

object Organisation {
  final case class OrgId(id: String)

  def fromJson(json: Json): Either[String, Organisation] = {
    val doc = json.hcursor

    doc
      .downField(FieldNames.PRIMARY_KEY)
      .as[String](Decoder[String].or(Decoder[Int].emap(r => Right(r.toString))))
      .leftMap(_.message)
      .map(id => Organisation(Field.fromJson(json), OrgId(id)))
  }

  implicit val orgId: Lens[Organisation, String] =
    Lens[Organisation, String](_.id.id)(int => ticket => ticket.copy(id = OrgId(int)))

  implicit val orgIdOptional: Optional[Organisation, Organisation.OrgId] =
    Optional[Organisation, Organisation.OrgId](t => Some(t.id))(a => s => s.copy(id = a))

  implicit val showOrganisation: Show[Organisation] =
    (t: Organisation) => s"""
                            |
                            |## ORGANISATION
                            |----------------------------------------
                            |${FieldsPrettyPrint(t.fields)}
                            |
                            |----------------------------------------
                            |
                            |""".stripMargin
}
