package com.zendesk.search

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.zendesk.search.repo.{ Field, IndexedInMemory }
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import fs2.Stream
import monocle.Lens

class IndexedInMemorySpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with TestSupport
    with ScalaCheckPropertyChecks
    with ArbitraryJsonInstance {
  "IndexedInMemory works for singleton data" - {
    "given two indexed in-memory the monoidal addition implements indexing" in {
      final case class A(id: Int, list: List[Field[Int, Int]])

      // Original data in doc
      val a1 = A(1, List(Field(2, 3)))

      // Data has an Id
      implicit val lens: Lens[A, Int] = Lens[A, Int](_.id)(a => b => b.copy(id = a))

      // A stream of singleton can be loaded to inmemory indexed repo
      IndexedInMemory.from(Stream.eval(IO.pure(a1)))(_.list)(v => List(v)).map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1))
        indexedInMemory.secondaryIndex shouldBe (Map(Field(2, 3) -> List(1)))
      }
    }
  }

  "IndexedInMemory merges into inverted index" - {
    "Given two indexed in-memory the monoidal addition implements indexing" in {
      final case class A(id: Int, list: List[Field[Int, Int]])

      // Original data in doc
      val a1 = A(1, List(Field(2, 3), Field(3, 4)))
      val a2 = A(2, List(Field(2, 3), Field(5, 6)))

      // Data has an Id
      implicit val lens: Lens[A, Int] = Lens[A, Int](_.id)(a => b => b.copy(id = a))

      // Related records will be merged with inverted index.
      IndexedInMemory.from(Stream.fromIterator[IO](List(a1, a2).iterator, 1))(_.list)(v => List(v)).map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1, 2 -> a2))
        indexedInMemory.secondaryIndex shouldBe (Map(Field(2, 3) -> List(1, 2), Field(3, 4) -> List(1), Field(5, 6) -> List(2)))
      }
    }
  }

  // Slighlty redundant yet good to keep it.
  "IndexedInMemory merges into inverted index for odd number of records" - {
    "Given three indexed in-memory the monoidal addition implements indexing" in {
      final case class A(id: Int, list: List[Field[Int, Int]])

      // Original data in doc
      val a1 = A(1, List(Field(2, 3), Field(3, 4)))
      val a2 = A(2, List(Field(2, 3), Field(5, 6)))
      val a3 = A(3, List(Field(5, 6), Field(3, 4)))

      // Data has an Id
      implicit val lens: Lens[A, Int] = Lens[A, Int](_.id)(a => b => b.copy(id = a))

      // Related records will be merged with inverted index.
      IndexedInMemory.from(Stream.fromIterator[IO](List(a1, a2, a3).iterator, 1))(_.list)(v => List(v)).map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1, 2 -> a2, 3 -> a3))
        indexedInMemory.secondaryIndex shouldBe (Map(Field(2, 3) -> List(1, 2), Field(3, 4) -> List(1, 3), Field(5, 6) -> List(2, 3)))
      }
    }
  }
}
