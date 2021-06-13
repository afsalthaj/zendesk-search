package com.zendesk.search.support

import io.circe.Json
import cats.syntax.eq._
import cats.instances.string._
import com.zendesk.search.repo.Field

trait JsonSyntax {
  implicit class JsonOps(json: Json) {

    /**
     * This is the place where each json value
     * is tokenized and that is then pushed to the index
     * from call-site
     */
    def decomposeString: List[String] = json.fold(
      List(""),
      r => List(r.toString),
      r => List(r.toString),
      s => s :: s.split(" ").filterNot(_ === "").toList,
      a => a.toList.flatMap(_.decomposeString),
      a => a.values.toList.flatMap(_.decomposeString)
    )

    def asFields: List[Field[String, Json]] =
      for {
        obj    <- json.asObject.toList
        (k, v) <- obj.toVector
      } yield Field(k, v)
  }
}
