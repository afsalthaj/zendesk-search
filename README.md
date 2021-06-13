# ZenDesk Search

## Core logic / Search

https://github.com/afsalthaj/zendesk-search/blob/master/src/main/scala/com/zendesk/search/repo/IndexedInMemory.scala#L29

```scala
abstract sealed case class IndexedInMemory[Id, K, V, A](
  primaryIndex: Map[Id, A],
  secondaryIndex: Map[Field[K, V], List[Id]]
)

```

### Write

Example: https://github.com/afsalthaj/zendesk-search/blob/master/src/test/scala/com/zendesk/search/IndexedInMemorySpec.scala#L38

The primary index is, for this use case can be, `Map[PrimaryKey, Json]`.
The secondary index is a map of search field `Field("country", "aus")` to the PrimaryKeys (Example: `Map(Field("country", "aus") -> List("1"))`),

Search fields from each `Json` (input is `JsonArray`) is obtained by
**decomposing Json structure**(https://github.com/afsalthaj/zendesk-search/blob/master/src/main/scala/com/zendesk/search/support/JsonSyntax.scala#L15).

After the write is finished, the secondary index will a map of search-field to a list of `PrimaryKey` of `Jsons` in the doc.
This complex logic is actually delegated to `Monoid[IndexedInMemory]`, and using it in the `foldMonoid` of `fs2.Stream`
(having to write less code)

### Read
The search query is essentially `Field[String, String]` (in this usecase).
That is, the search term is a string, and the search value is also a string. Example: `Field("_id", "1")`.
Result of this query can be obtained by first hitting the secondary index (inverted index) to get the list
of `ids`, and traversing the `ids` to hit the primary to further obtain the real `Json` values.


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


[nix-shell:~/github/zendesk-search/target/scala-2.13]$ ./zendesk-search
Type 'quit' to exit any time, Press Enter to continue

Select 1 for Users, 2 for Tickets, or 3 for Organizations
1
Enter search term
_id
Enter search value
1
Searching users...


## USER
----------------------------------------
_id              :  1
url              :  "http://initech.zendesk.com/api/v2/users/1.json"
external_id      :  "74341f74-9c79-49d5-9611-87ef9b6eb75f"
name             :  "Francisca Rasmussen"
alias            :  "Miss Coffey"
created_at       :  "2016-04-15T05:19:46 -10:00"
active           :  true
verified         :  true

....

## TICKET
...

## ORGANISATION
...

## ORGANISATION
...

```

## Quick usecase overview

The application is specific to searching entities across
three documents that are as follows

1) Users
2) Organisations
3) Tickets

This should be existing as Json files which you will have to pass as command line arguments
 
## Assumptions

* Search is basic. There is no prefix/suffix/like search. There is no sorted keys in index to enable binary search.
* Search is always per key. You have to enter the key (search term) first to then search for the value.
* Search lists down all the information (all field values) of all related entities (instead of assuming a particular field).
* Json decoding is minimised as much as possible, instead of decoding it to strictly typed data (case classes). All the entities
are assumed to have a required primary-key, and an optional organization_id.
 
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
The patterns are subjected to change based on PR reviews from the team.

## What could be improved?

* More test opportunities are there.
* More code clean up and bring about consistency.
* FieldNames.scala could be avoided somehow.
* More bullet-proof error handling with Monad Transformers. Intentionally avoided for the time being.
* Higher kinded effect system is not being used yet, since for the most part, it is in-memory repository. This could change though (and also based on team's preference)
* Dependency injection is not given much priority yet. This can be a bottle-neck if the app expands and requires
external services (DynamoDb Client, ElasticSearch, S3Client/AWS SDK etc) in future.
* Reduce more lines if possible.
* Handling partial writes (when the repo is Elastic-search for instance), but this is for future.
