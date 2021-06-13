package com.zendesk.search.support

import cats.Show
import io.circe.Json
import cats.syntax.show._

trait JsonShowInstance extends JsonOps {
  implicit val show: Show[Json] = (t: Json) => {
    t.fold(
      "",
      t => t.toString,
      t => t.toString,
      identity,
      t => t.map(_.show).mkString(", "),
      t => t.show
    )
  }
}
