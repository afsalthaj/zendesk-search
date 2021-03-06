package com.zendesk.search.repo

import fs2.Stream
import cats.effect.IO
import cats.syntax.monoid._
import cats.instances.list._
import cats.kernel.Monoid
import cats.syntax.foldable._
import monocle.Lens

abstract sealed case class IndexedInMemory[Id, K, V, A](
  primaryIndex: Map[Id, A],
  searchIndex: Map[Field[K, V], List[Id]]
)

object IndexedInMemory {

  /**
   * Defining given two IndexedInMemory, how to squash them.
   * It is a monoidal additions of Map
   */
  implicit def monoidOfIndexedInMemory[Id, K, V, A] = new Monoid[IndexedInMemory[Id, K, V, A]] {
    def combine(
      x: IndexedInMemory[Id, K, V, A],
      y: IndexedInMemory[Id, K, V, A]
    ): IndexedInMemory[Id, K, V, A]         =
      new IndexedInMemory((x.primaryIndex ++ y.primaryIndex), x.searchIndex |+| y.searchIndex) {}

    def empty: IndexedInMemory[Id, K, V, A] =
      new IndexedInMemory[Id, K, V, A](Map.empty, Map.empty) {}
  }

  def from[Id, K, V1, V2, A](
    datastream: Stream[IO, A]
  )(
    f: A => List[Field[K, V1]]
  )(
    g: V1 => List[V2]
  )(implicit
    HasPrimaryKey: Lens[A, Id]
  ): IO[IndexedInMemory[Id, K, V2, A]] =
    datastream
      .map(a => singletonIndexedMemory(HasPrimaryKey.get(a), a)(f)(g))
      .foldMonoid
      .compile
      .toList
      .map(r => r.foldMap(identity))

  def singletonIndexedMemory[Id, K, V1, V2, A](
    key: Id,
    data: A
  )(
    f: A => List[Field[K, V1]]
  )(
    g: V1 => List[V2]
  ): IndexedInMemory[Id, K, V2, A] =
    new IndexedInMemory[Id, K, V2, A](
      Map(key -> data), {
        val allFieldsInA: List[Field[K, V1]] = f(data)
        val v1Tokenized: List[Field[K, V2]]  =
          allFieldsInA.flatMap(field => g(field.v).map(v => Field(field.k, v)))
        v1Tokenized.map(field => (field, List(key))).toMap
      }
    ) {}
}
