package com.zendesk.search

import com.zendesk.search.model.Organisation.OrgId
import com.zendesk.search.model.{ Organisation, Ticket, User }
import org.scalacheck.{ Arbitrary, Gen }

trait Arbitraries {
  implicit val arbitraryUser: Arbitrary[User]        =
    Arbitrary {
      Gen.const(User(Nil, "id", None))
    }

  implicit def arbitraryOrg: Arbitrary[Organisation] =
    Arbitrary {
      Gen.const(Organisation(Nil, OrgId("id")))
    }

  implicit def arbitraryTicket: Arbitrary[Ticket]    =
    Arbitrary {
      Gen.const(Ticket(Nil, "id", None))
    }
}
