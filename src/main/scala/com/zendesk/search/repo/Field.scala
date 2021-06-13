package com.zendesk.search.repo

import io.circe.Json

final case class Field[K, V](k: K, v: V)

object Field {
  def fromJson(json: Json): List[Field[String, Json]] =
    for {
      obj    <- json.asObject.toList
      (k, v) <- obj.toVector
    } yield Field(k, v)
}
