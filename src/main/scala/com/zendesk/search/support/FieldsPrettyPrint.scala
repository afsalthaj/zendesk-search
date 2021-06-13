package com.zendesk.search.support

import cats.Show
import com.zendesk.search.repo.Field
import cats.syntax.show._

object FieldsPrettyPrint {
  def apply[K: Show, V: Show](list: List[Field[K, V]]): String = {
    val maxKeySize = list.map(_.k.show.length).max

    val paddedList =
      list.map { r =>
        s"${r.k.show.padTo(maxKeySize, " ").mkString}  :  ${r.v.show}"
      }

    paddedList.mkString("\n")
  }
}
