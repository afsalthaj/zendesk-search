## Installation

### Pre-requisite

Please make sure the following are available in your system. 

* Java11
* sbt  

If you are familiar/interested in nix, read through the optional nix session,
as a way to get it done. 

### Set up Environment using Nix (Optional)

Make sure nix is in your system: https://nixos.org/download.html
During installation, make sure that you add the nix to your bash profile.

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

## Core logic / Search

https://github.com/afsalthaj/zendesk-search/blob/master/src/main/scala/com/zendesk/search/repo/IndexedInMemory.scala

The main idea here is, the data is streamed (fs2-stream) and aggregated into an indexed in-memory database. 
The primary index is primary key of each data and the value is the data itself represented as Json.
The secondary index will be keyed upon search string (obtained by decomposing each json value), and the value will be list of indices.

The search query is essentially a `Field[String, String]`. That is, the search term is a string, and the value is also a string.
Result of this query can be obtained by first hitting the secondary index (inverted index) and then hitting the primary index
to get the actual values.

## Usage

```scala
cd target/scala-2.13
./zendesk-search [press enter]
// Also, try, ./zendesk-search --help

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

* Search is basic. There is no prefix/suffix/like search.
* Search is always per key. You have to enter the key (search term) first to then search for the value
* Search lists down all the information (all field values) of all related entities (instead of assuming a particular field)
* Json decoding is minimised as much as possible, instead of decoding it to strictly typed data (case classes). All the entities
are assumed to have a required primary-key, and an optional organization_id.
 
## Libraries used

Libraries used are predominantly from Typelevel ecosystem. 

* decline     
* monocle     
* fs2         
* circe        
* catsScalaTes
* scalaTestPlus
* catsLawSpec 

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
* Too many lines of code based on self-review. I could reduce it further.
