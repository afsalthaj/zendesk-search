package com.zendesk.search

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.zendesk.search.repo.{ Field, IndexedInMemory, Repo }
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

  // Generate a whole bunch of input represented as Map[A, Map[B, List[C]]
  // where A is the primary key of the data Map[B, List[C]] and B is the field name.
  // Example: Test can generate Map("" -> Map("" -> List())), and see how search index behaves, covering empty string search!
  "IndexedInMemory Content" - {
    "All primary keys are available in primaryIndex, and all field names are available in searchIndex" in {
      // This implies if a line of record is a tuple with first element a primary key
      // there exists a lens. In below case, this tuple (String, Map[String, List[String]])
      implicit def lensTuple[A, B]: Lens[(A, B), A] =
        Lens[(A, B), A](_._1)(a => b => (a, b._2))

      forAll { (data: Map[String, Map[String, List[String]]]) =>
        val indexedInMemory =
          IndexedInMemory
            .from[String, String, List[String], String, (String, Map[String, List[String]])](Stream.fromIterator[IO](data.iterator, 1))(v =>
              v._2.map({ case (k, v) => Field(k, v) }).toList
            )(identity)
            .unsafeRunSync()(cats.effect.unsafe.implicits.global)

        def fieldsWithValuesNonEmpty[A]: Map[String, List[A]] => List[String] =
          _.filterNot(_._2.isEmpty).keys.toList

        val fieldValues =
          data.values.toList

        val inputFields =
          fieldValues.flatMap(fieldsWithValuesNonEmpty).distinct

        val repo = Repo.fromIndexedInMemory(indexedInMemory)

        indexedInMemory.primaryIndex.keys.toList shouldBe (data.keys.toList)
        indexedInMemory.searchIndex.keys.toList.map(_.k).distinct.sorted shouldBe (inputFields.sorted)
      }
    }
  }

  /**
   * Explanation:
   * We know, List[(Field, List[V]]) can be an example summary of a simple json object.
   * The Field is each key in JsonObject, and value can be tokenised to a List[V]. Example:  { key: [v1, v2, v3] }
   * In this test case, we keep the Field to be a constant (fieldKey), so that we can easily check if the property holds.
   */
  "IndexedInMemory.singletonIndexedMemory" - {
    "the value of each key in the json object will be further tokenised before pushing to searchIndex" in {
      forAll { (primaryKey: String, v: List[List[String]], fieldKey: String) =>
        val indexedInMemory: IndexedInMemory[String, String, String, List[List[String]]] =
          IndexedInMemory.singletonIndexedMemory(primaryKey, v)(a => a.map(list => Field(fieldKey, list)))(identity)

        indexedInMemory.primaryIndex shouldBe (Map(primaryKey -> v))
        indexedInMemory.searchIndex shouldBe (v.flatten.map(str => (Field(fieldKey, str), List(primaryKey))).toMap)
      }
    }
  }

  "IndexedInMemory works for empty fields" - {
    "given a single indexed in-memory with empty fields, the monoidal addition implements indexing" in {
      final case class A(id: Int, list: List[Field[Int, Int]])

      // Original data in doc
      val a1 = A(1, Nil)

      // Data has an Id
      implicit val lens: Lens[A, Int] = Lens[A, Int](_.id)(a => b => b.copy(id = a))

      // A stream of singleton can be loaded to inmemory indexed repo
      IndexedInMemory.from(Stream.eval(IO.pure(a1)))(_.list)(v => List(v)).map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1))
        indexedInMemory.searchIndex shouldBe (Map())
      }
    }
  }

  "IndexedInMemory works for singleton data with non-empty fields" - {
    "given a single indexed in-memory the monoidal addition implements indexing" in {
      final case class A(id: Int, list: List[Field[Int, Int]])

      // Original data in doc
      val a1 = A(1, List(Field(2, 3)))

      // Data has an Id
      implicit val lens: Lens[A, Int] = Lens[A, Int](_.id)(a => b => b.copy(id = a))

      // A stream of singleton can be loaded to inmemory indexed repo
      IndexedInMemory.from(Stream.eval(IO.pure(a1)))(_.list)(v => List(v)).map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1))
        indexedInMemory.searchIndex shouldBe (Map(Field(2, 3) -> List(1)))
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
        indexedInMemory.searchIndex shouldBe (Map(Field(2, 3) -> List(1, 2), Field(3, 4) -> List(1), Field(5, 6) -> List(2)))
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
        indexedInMemory.searchIndex shouldBe (Map(Field(2, 3) -> List(1, 2), Field(3, 4) -> List(1, 3), Field(5, 6) -> List(2, 3)))
      }
    }
  }
}
