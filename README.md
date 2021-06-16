# ZenDesk Search

[![Build Status](https://travis-ci.com/afsalthaj/zendesk-search.svg?branch=master)](https://travis-ci.com/afsalthaj/zendesk-search)

## Core logic / Search

https://github.com/afsalthaj/zendesk-search/blob/master/src/main/scala/com/zendesk/search/repo/IndexedInMemory.scala

```scala
abstract sealed case class IndexedInMemory[Id, K, V, A](
  primaryIndex: Map[Id, A],
  searchIndex: Map[Field[K, V], List[Id]]
)

```

### Write

The primary index is `Map[PrimaryKey, Json]`. 
Note that, the core-logic is independent of data being a `Json`, or `PrimaryKey` being a String. They are polymorphic.

The search index is a `Map[Field[String, String], List[PrimaryKey]]`. Example: `Map(Field("country", "aus") -> List("1"))`.
Again, the core-logic is independent of field (key-value pair) being a `(String, String)`.

### Additional details
Search fields from each `Json` is obtained by 
**tokenising/decomposing the values in Json structure per primary-key** 

Tokenization is here: https://github.com/afsalthaj/zendesk-search/blob/master/src/main/scala/com/zendesk/search/support/JsonSupport.scala#L16.

After data-write to (in-memory) store is finished, 
the search index will be a `map` of `Field[String, String]` to a list of `PrimaryKey`, so that look-up is faster.
The concatenation of list of index per `Field[String, String]` (i.e, `(search-field, search-value)`) 
is done by the `Monoid` instance of `IndexedInMemory`, which is then used with `foldMonoid` of `fs2.Stream`.

`fs2.Stream` approach will help us to sink the data to a better store in near-future (say, elastic-search) without grabbing the whole
data into memory, while still allowing fine-grained control over (side) effect-ful computation 
(Example: error handling especially talking to outside-world, concurrency/parallel-writes-to-partitions) 
backed by `cats.effect.IO`.

### Read
The search query is essentially `Field[String, String]` (Note: The core logic is independent of `String`s).

That is, the search term is a string, and the search value is also a string. Example: `Field("_id", "1")`.
Result of this query can be obtained by first hitting the search index (inverted index) to get the list
of `ids`, and traversing the `ids` to hit the primary-index to further obtain the real `Json` values (Note: The core logic is independent of the data being a `Json`).


## Installation

### Pre-requisite

Please make sure the following are available in your system. 

* Java11
* sbt  

If you are familiar/interested in nix, read through the optional nix session,
as a way to get it done. 

### Set up Environment using Nix (Optional)

Make sure nix is in your system: https://nixos.org/download.html.

Note that during installation, it will suggest you to add nix to your profile.

```scala
git clone https://github.com/leigh-perry/nix-setup.git
cd nix-setup
nix-shell scala.nix
cd .. // Come out of nix repo
```

### Build

Build the application using `sbt assembly`

```scala
git clone https://github.com/afsalthaj/zendesk-search.git
cd zendesk-search
sbt assembly
```

## Usage

```scala
cd target/scala-2.13
./zendesk-search --org-path ../../data/organizations.json --ticket-path "../../data/tickets.json" --user-file "../../data/users.json"
// Also try ./zendesk-search --help
```

### Example:

```scala

Type 'quit' to exit any time, Press Enter to continue

Select 1 for Users, 2 for Tickets, or 3 for Organizations
2
Enter search term
subject
Enter search value
Zimbabwe
Searching tickets...


## TICKET
----------------------------------------
_id              :  c702e937-5f2d-4d34-878a-fcb7d1ddf6aa
url              :  http://initech.zendesk.com/api/v2/tickets/c702e937-5f2d-4d34-878a-fcb7d1ddf6aa.json
external_id      :  5e349072-774b-4c77-ba63-61bca77c82ff
created_at       :  2016-05-25T12:48:45 -10:00
type             :  question
subject          :  A Drama in Zimbabwe

...
```

## Quick usecase overview

The application is specific to searching entities across
three documents that are as follows

1) Users
2) Organisations
3) Tickets

This should be existing as Json files which you will have to pass as command line arguments
 
## Assumptions/Boundaries

* Search is basic. There is no prefix/suffix/like search. There is no sorted keys in index to enable binary search.
* Search is always per key. You have to enter the key (search term) first to then search for the value.
* Search lists down all the information (all field values) of all related entities (instead of assuming a particular field). This can be easily/confidently changed as well since we hardly use `toString` anywhere.
* Json decoding is minimised as much as possible, instead of decoding it to strictly typed data (case classes). All the entities
are assumed to have a required primary-key, and an optional organization_id.
* Cross document search could also be implemented by wrapping in-memory index to a doc, however, this is avoided
unless it is really required.

 
## Libraries used

Libraries used are predominantly from Typelevel ecosystem. 

* decline     
* monocle     
* fs2         
* circe        
* catsScalaTest
* scalaTestPlus

The libraries are subjected to change based on PR reviews from the team.

## Why IO effect?

The interfaces for the most part returns an actual effect `IO`.
The exact effect system (or changing it to even `Either[String, Result]`) can happen based on team's preference and PR reviews.

## Why polymorphic parameterization all over the place?

As much as possible, parameteric polymorphism is being used to improve reasonability.
The patterns are subjected to change based on PR reviews/preference from the team.

## What could be improved?

* More tests required (RepoSpec)
* FieldNames.scala could be avoided somehow.
* More bullet-proof error handling with Monad Transformers. Intentionally avoided for the time being.
* Higher kinded effect system is not being used yet, since for the most part, it is in-memory repository. This could change though (and also based on team's preference)
* Dependency injection is not given much priority yet. But we are not worrying about it now given the scope of the problem forthe time being.
* Reduce more code lines if possible. Some parts of the code could be cleaned up to reduce the lines of code and project the core logic better.
* Handling partial writes (when the repo is Elastic-search for instance), but this is for future.
* scala-3 could have reduced the boiler-plate looking implicits.

## Cross Document Search
Cross document search is kept on hold until it is required. This is being able to search for a particular
search field and value, and bring the information where that appears across all documents. 
This is fairly possible, by doing something along the below lines (of pseudoish code)

```scala

final case class Doc[DocId, Id, K, V, A](
  id: Map[DocId, IndexedInMemory[Id, K, V, A]], 
  docSearch: Map[Field[K, V], List[DocId]]
)

object Doc {
  implicit def monoidOfDoc[DocId, Id, K, V, A] = new Monoid[Doc[DocId, Id, K, V, A]] {
    override def combine(x: Doc[DocId, Id, K, V, A], y: Doc[DocId, Id, K, V, A]): Doc[DocId, Id, K, V, A] =
      Doc((x.id ++ y.id), x.docSearch |+| y.docSearch)
    override def empty: Doc[DocId, Id, K, V, A]                                                           =
      Doc(Map.empty, Map.empty)
  }
}
```

Each document write to its own specific index returns a `Doc` as the output,
where `docSearch` field will be a map of each search field to that particular `DocId` wrapped in a `List` (so that we can later merge).

After all the documents are written to the their own `IndexedInMemory`, 
we will have a `List[Doc]` which we can easily squash, resulting in each field mapped to the list of `DocId`s.