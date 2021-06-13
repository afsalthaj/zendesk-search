package com.zendesk.search

import com.zendesk.search.model.Organisation.OrgId
import com.zendesk.search.model.{ Organisation, Ticket, User }
import org.scalacheck.{ Arbitrary, Gen }
import _root_.io.circe.Json
import _root_.io.circe._

trait ArbitraryInstances {
  def maxJsonArraySize: Int  = 10
  def maxJsonDepth: Int      = 5
  def maxJsonObjectSize: Int = 10

  def genNull: Gen[Json]   = Gen.const(Json.Null)
  def genBool: Gen[Json]   = Arbitrary.arbitrary[Boolean].map(Json.fromBoolean)
  def genString: Gen[Json] = Arbitrary.arbitrary[String].map(Json.fromString)
  def genNumber: Gen[Json] = Arbitrary.arbitrary[JsonNumber].map(Json.fromJsonNumber)

  def genJsonAtDepth(depth: Int): Gen[Json] = {
    val genJsons = List(genNumber, genString) ++ (
      if (depth < maxJsonDepth) List(genArray(depth), genJsonObject(depth).map(Json.fromJsonObject)) else Nil
    )

    Gen.oneOf(genNull, genBool, genJsons: _*)
  }

  def genJsonObject(depth: Int): Gen[JsonObject] = Gen.choose(0, maxJsonObjectSize).flatMap { size =>
    val fields = Gen.listOfN(
      size,
      for {
        key   <- Arbitrary.arbitrary[String]
        value <- genJsonAtDepth(depth + 1)
      } yield key -> value
    )

    Gen.oneOf(
      fields.map(JsonObject.fromIterable),
      fields.map(JsonObject.fromFoldable[List])
    )
  }

  def orgId =
    Gen.choose[Int](1, 10).map(r => Organisation.OrgId(r.toString))

  def primaryKey: Gen[String] =
    Gen.choose[Int](1, 5).flatMap(n => Gen.listOfN(n, Gen.alphaChar)).map(_.mkString)

  def genArray(depth: Int): Gen[Json] = Gen.choose(0, maxJsonArraySize).flatMap { size =>
    Gen.listOfN(size, genJsonAtDepth(depth + 1)).map(Json.arr)
  }

  implicit val arbitraryJsonNumber: Arbitrary[JsonNumber] = Arbitrary(
    Gen
      .oneOf(
        Arbitrary.arbitrary[BigDecimal].map(Json.fromBigDecimal(_).asNumber.get),
        Arbitrary.arbitrary[BigInt].map(Json.fromBigInt(_).asNumber.get),
        Arbitrary.arbitrary[Long].map(Json.fromLong(_).asNumber.get),
        Arbitrary.arbitrary[Double].map(Json.fromDoubleOrString(_).asNumber.get),
        Arbitrary.arbitrary[Float].map(Json.fromFloatOrString(_).asNumber.get)
      )
  )

  implicit val arbitraryJson: Arbitrary[Json] = Arbitrary(genJsonAtDepth(0))

}
