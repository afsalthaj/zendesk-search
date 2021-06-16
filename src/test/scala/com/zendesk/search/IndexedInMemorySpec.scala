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

  /**
   * Generate a whole bunch of input data represented as `Map[A, Map[B, List[C]]`.
   * Example it can generate Map("" -> Map("" -> List())), and see how search index behaves, covering empty string search.
   *
   * In, {{{ Map[A, Map[B, List[C]]] }}},
   * A is the primary key of the data.
   * `Map[B, List[C]]` is the data itself, which is a key-value pair where key (field-name) is `B`
   * This is similar to { "id" : a, "b" : ["c1", "c2"] }
   */
  "IndexedInMemory Content" - {
    "All primary keys are available in primaryIndex, and all field names are available in searchIndex" in {
      forAll { (data: Map[String, Map[String, List[String]]]) =>
        val indexedInMemory =
          IndexedInMemory
            .from[String, String, List[String], String, (String, Map[String, List[String]])](Stream.fromIterator[IO](data.iterator, 1))(v =>
              v._2.map({ case (k, v) => Field(k, v) }).toList
            )(identity)
            .runIO

        def fieldsWithValuesNonEmpty[A]: Map[String, List[A]] => List[String] =
          _.filterNot(_._2.isEmpty).keys.toList

        val fieldValues =
          data.values.toList

        val inputFields =
          fieldValues.flatMap(fieldsWithValuesNonEmpty).distinct

        indexedInMemory.primaryIndex.keys.toList shouldBe (data.keys.toList)
        indexedInMemory.searchIndex.keys.toList.map(_.k).distinct.sorted shouldBe (inputFields.sorted)
      }
    }
  }

  "IndexedInMemory works for empty fields" - {
    "given a single indexed in-memory with empty fields, the monoidal addition implements indexing" in {
      type Data = (Int, List[Field[Int, Int]])

      // Original data in doc
      val a1: Data = (1, Nil)

      val stream =
        Stream.eval(IO.pure(a1))

      val indexedInMemoryIO =
        IndexedInMemory.from(stream)(_._2)(v => List(v))

      // A stream of singleton can be loaded to inmemory indexed repo
      indexedInMemoryIO.map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1))
        indexedInMemory.searchIndex shouldBe (Map())
      }
    }
  }

  "IndexedInMemory works for singleton data with non-empty fields" - {
    "given a single indexed in-memory the monoidal addition implements indexing" in {
      type Data = (Int, List[Field[Int, Int]])

      // Original data in doc
      val a1: Data = (1, List(Field(2, 3)))

      // A stream of singleton data
      val stream =
        Stream.eval(IO.pure(a1))

      val indexedInMemoryIO =
        IndexedInMemory.from(stream)(_._2)(v => List(v))

      indexedInMemoryIO.map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1))
        indexedInMemory.searchIndex shouldBe (Map(Field(2, 3) -> List(1)))
      }
    }
  }

  "IndexedInMemory merges into inverted index" - {
    "Given two indexed in-memory the monoidal addition implements indexing" in {
      type Data = (Int, List[Field[Int, Int]])

      // Original data in doc
      val a1: Data = (1, List(Field(2, 3), Field(3, 4)))
      val a2: Data = (2, List(Field(2, 3), Field(5, 6)))

      val stream =
        Stream.fromIterator[IO](List(a1, a2).iterator, 1)

      val indexedInMemoryIO =
        IndexedInMemory.from(stream)(_._2)(v => List(v))

      // Related records will be merged with inverted index.
      indexedInMemoryIO.map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1, 2 -> a2))
        indexedInMemory.searchIndex shouldBe (Map(Field(2, 3) -> List(1, 2), Field(3, 4) -> List(1), Field(5, 6) -> List(2)))
      }
    }
  }

  // Slighlty redundant yet good to keep it.
  "IndexedInMemory merges into inverted index for odd number of records" - {
    "Given three indexed in-memory the monoidal addition implements indexing" in {

      type Data = (Int, List[Field[Int, Int]])

      // Original data in doc
      val a1 = (1, List(Field(2, 3), Field(3, 4)))
      val a2 = (2, List(Field(2, 3), Field(5, 6)))
      val a3 = (3, List(Field(5, 6), Field(3, 4)))

      val stream =
        Stream.fromIterator[IO](List(a1, a2, a3).iterator, 1)

      // Related records will be merged with inverted index.
      IndexedInMemory.from(stream)(_._2)(v => List(v)).map { indexedInMemory =>
        indexedInMemory.primaryIndex shouldBe (Map(1 -> a1, 2 -> a2, 3 -> a3))
        indexedInMemory.searchIndex shouldBe (Map(Field(2, 3) -> List(1, 2), Field(3, 4) -> List(1, 3), Field(5, 6) -> List(2, 3)))
      }
    }
  }

  /**
   * Note:
   * We know, List[(Field, List[V]]) can be an example summary of a simple json object.
   * The `Field` is each key in JsonObject, and value can be tokenised to a `List[V]`. Example:  { key: [v1, v2, v3] }
   * In this test case, we keep the value of `Field` to be a constant (generated randomly called `fieldKey`),
   * so that we can easily check if the property holds.
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
}
