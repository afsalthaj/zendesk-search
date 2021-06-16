package com.zendesk.search
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import _root_.io.circe.Json
import com.zendesk.search.support.JsonSupport

import scala.util.Try

//TODO: Should also have more specific explicit tests too.
class JsonSupportSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with JsonSupport
    with Matchers
    with TestSupport
    with ScalaCheckPropertyChecks
    with ArbitraryJsonInstance {
  "Json String Tokenization" - {
    "All types of Json can be decomposed to strings" in {
      forAll { (json: Json) =>
        Try {
          json.tokeniseJson
        }.toEither.isRight shouldBe (true)
      }
    }
  }

  "json.asFields functionality shouldn't make assumptions on json structure and fail abruptly" - {
    "All types of Json can be converted to List[Field[String, Json]]" in {
      forAll { (json: Json) =>
        Try {
          json.asFields
        }.toEither.isRight shouldBe (true)
      }
    }
  }
}
