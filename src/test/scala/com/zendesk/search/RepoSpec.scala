package com.zendesk.search

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.zendesk.search.repo.{ Field, IndexedInMemory, Repo }
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import fs2.Stream
import monocle.Lens
import cats.syntax.foldable._
import cats.instances.map._
import cats.instances.list._
import cats.syntax.traverse._

import scala.collection.immutable
import org.scalactic.Bool

class RepoSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with TestSupport
    with ScalaCheckPropertyChecks
    with ArbitraryJsonInstance {

  "Repo: A data where value of all primary keys have the same data." - {
    "Query of any search-field-search-term returns the full data" in {
      forAll { (commonValue: Map[String, List[String]], arbitraryPks: List[String]) =>
        // pks should be distinct
        val pks = arbitraryPks.distinct

        val fullData: Map[String, Map[String, List[String]]]     =
          pks.foldMap(pk => Map(pk -> commonValue))

        val allSearchNameSearchTerm: List[Field[String, String]] =
          commonValue.toList
            .flatMap(
              { case (fieldName, fieldValues) =>
                fieldValues.map(fieldValue => Field(fieldName, fieldValue))
              }
            )

        val allQueriesReturnsSameData: IO[Boolean] =
          for {
            repo    <- Repo.indexedInMemoryRepo(
                         Stream.fromIterator[IO](fullData.iterator, 1)
                       )({ case (_, data) => data.map({ case (fieldName, value) => Field(fieldName, value) }).toList })(identity)

            allData <- allSearchNameSearchTerm.traverse(field => repo.query(field).map(_.toMap))
          } yield allData.forall(d => d == fullData)

        allQueriesReturnsSameData.runIO shouldBe true
      }
    }
  }
}
