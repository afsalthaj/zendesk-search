package com.zendesk.search

import com.zendesk.search.model.{ FieldNames, Organisation, Ticket, User }
import org.scalacheck.{ Arbitrary, Gen }
import _root_.io.circe.Json
import com.zendesk.search.support.JsonSupport

trait ArbitraryEntities extends ArbitraryJsonInstance with JsonSupport {
  def orgId: Gen[Organisation.OrgId] =
    Gen.choose[Int](1, 10).map(r => Organisation.OrgId(r.toString))

  def primaryKey: Gen[String] =
    Gen.choose[Int](1, 5).flatMap(n => Gen.listOfN(n, Gen.alphaChar)).map(_.mkString)

  def addKeyValue(key: String, value: String): Json => Json =
    _.mapObject(_.+:(key, Json.fromString(value)))

  implicit val arbitraryUser: Arbitrary[User] =
    Arbitrary {
      for {
        orId     <- Gen.option(orgId)
        id       <- primaryKey
        json     <- Arbitrary.arbitrary[Json]
        withKey   = addKeyValue(FieldNames.PRIMARY_KEY, id)(json)
        withOrgId = orId match {
                      case Some(value) => addKeyValue(FieldNames.ORG_ID, value.id)(withKey)
                      case None        => withKey
                    }

        fields = withOrgId.asFields
      } yield User(fields, id, orId)
    }

  implicit val arbitraryOrganisation: Arbitrary[Organisation] =
    Arbitrary {
      for {
        orId   <- orgId
        json   <- Arbitrary.arbitrary[Json]
        withKey = addKeyValue(FieldNames.PRIMARY_KEY, orId.id)(json)
        fields  = withKey.asFields
      } yield Organisation(fields, orId)
    }

  implicit def arbitraryTicket: Arbitrary[Ticket] =
    Arbitrary {
      for {
        orId     <- Gen.option(orgId)
        id       <- primaryKey
        json     <- Arbitrary.arbitrary[Json]
        withKey   = addKeyValue(FieldNames.PRIMARY_KEY, id)(json)
        withOrgId = orId match {
                      case Some(value) => addKeyValue(FieldNames.ORG_ID, value.id)(withKey)
                      case None        => withKey
                    }

        fields = withOrgId.asFields
      } yield Ticket(fields, id, orId)
    }
}

object ArbitraryEntities {
  final case class UserInput(user: User, relatedTickets: List[Ticket], relatedOrganisations: List[Organisation])
  final case class TicketInput(ticket: Ticket, relatedUsers: List[User], relatedOrganisations: List[Organisation])
  final case class OrganisationInput(ticket: Organisation, relatedUsers: List[User], relatedOrganisations: List[Ticket])
}
