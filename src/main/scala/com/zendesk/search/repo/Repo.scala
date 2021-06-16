package com.zendesk.search.repo

import fs2.Stream
import cats.effect.IO
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.option._
import monocle.Lens

/**
 * PK: Primary Key (possibly)
 * Q : Query.
 *  For an in-memory it could be just a Field (querying for a specific key-value)
 *  For dynamodb, it could be Hashmap of hash key and sort key as a map (as an example)
 *  For a simple jdbc, it could be a doobie sql Fragment.
 *  For the last 2 examples, computation will become effectful
 *  and can even fail (ex: network exists between can fail)
 *
 * Repo is fully parametrically polymorphic indicating Repo doesn't change/manipulate the values
 * internally, other than fetching them from the underlying store.
 */
trait Repo[PK, Q, A] {
  def id(id: PK): IO[Option[A]]
  def query(query: Q): IO[List[A]]
}

object Repo {
  def from[PK, Q, A](getA: PK => IO[Option[A]], getAs: Q => IO[List[A]]): Repo[PK, Q, A] = new Repo[PK, Q, A] {
    def id(id: PK): IO[Option[A]]    = getA(id)
    def query(query: Q): IO[List[A]] = getAs(query)
  }

  def empty[PK, Q, A]: Repo[PK, Q, A] =
    Repo.from[PK, Q, A](_ => IO.pure(None), _ => IO.pure(Nil))

  def indexedInMemoryRepo[Id, K, V1, V2, A](
    stream: Stream[IO, A]
  )(
    f: A => List[Field[K, V1]]
  )(
    g: V1 => List[V2]
  )(implicit
    H: Lens[A, Id]
  ): IO[Repo[Id, Field[K, V2], A]] =
    IndexedInMemory.from[Id, K, V1, V2, A](stream)(f)(g).map(fromIndexedInMemory)

  def fromIndexedInMemory[A, K, V, Id](
    inMemory: IndexedInMemory[Id, K, V, A]
  ): Repo[Id, Field[K, V], A] =
    from(
      id => IO.pure(inMemory.primaryIndex.get(id)),
      field =>
        IO.pure(
          (for {
            ids <- inMemory.searchIndex.get(field)
            v   <- ids.traverse(id => inMemory.primaryIndex.get(id))
          } yield v).toList.flatten
        )
    )

  // Demonstrating it is extensible for any underlying store (data structure).
  // Also, if write/indexing takes a lot of time probably someone can choose
  // to use simple in-memory.
  type SubOptimalQuery[A] = A => Boolean

  def inMemoryRepo[A, Id](stream: Stream[IO, A])(implicit H: Lens[A, Id]): IO[Repo[Id, Repo.SubOptimalQuery[A], A]] =
    stream.compile.toList.map(r => fromInMemory(InMemory(r)))

  def fromInMemory[A, Id](
    inMemory: InMemory[A]
  )(implicit H: Lens[A, Id]): Repo[Id, SubOptimalQuery[A], A] = new Repo[Id, SubOptimalQuery[A], A] {
    override def id(id: Id): IO[Option[A]] =
      query(a => H.get(a) == id).map(_.headOption)

    override def query(query: SubOptimalQuery[A]): IO[List[A]] =
      IO.pure(inMemory.data.filter(query))
  }
}
