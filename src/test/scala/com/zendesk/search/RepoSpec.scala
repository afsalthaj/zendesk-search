package com.zendesk.search

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.zendesk.search.repo.{ Field, IndexedInMemory, Repo }
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import fs2.Stream
import monocle.Lens

class RepoSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with TestSupport
    with ScalaCheckPropertyChecks
    with ArbitraryJsonInstance {

  // Most of the tests are in IndexedInMemorySpec
  "Repp roundtrip" - {
    "All primary keys are available in Repo" in {
      // This implies if a single record of data is a tuple with primary key as the first element,
      // In below particular test case, this tuple is actually (String, Map[String, List[String]])
      implicit def lensTuple[A, B]: Lens[(A, B), A] =
        Lens[(A, B), A](_._1)(a => b => (a, b._2))

      forAll { (data: Map[String, Map[String, List[String]]]) =>
        val indexedInMemory =
          IndexedInMemory
            .from[String, String, List[String], String, (String, Map[String, List[String]])](Stream.fromIterator[IO](data.iterator, 1))(v =>
              v._2.map({ case (k, v) => Field(k, v) }).toList
            )(identity)
            .unsafeRunSync()(cats.effect.unsafe.implicits.global)

        val repo =
          Repo.fromIndexedInMemory(indexedInMemory)

        val existsInRepo: String => Boolean =
          repo.id(_).map(_.isDefined).unsafeRunSync()(cats.effect.unsafe.implicits.global)

        data.keys.toList.forall(existsInRepo) shouldBe (true)
      }
    }
  }
}
