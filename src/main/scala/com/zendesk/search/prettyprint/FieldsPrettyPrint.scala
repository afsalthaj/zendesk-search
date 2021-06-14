package com.zendesk.search.prettyprint

import cats.Show
import cats.syntax.show._
import com.zendesk.search.repo.Field

object FieldsPrettyPrint {
  def apply[K: Show, V: Show](list: List[Field[K, V]]): String =
    if (list.isEmpty)
      ""
    else {
      val maxKeySize = list.map(_.k.show.length).max

      val paddedList =
        list.map { r =>
          s"${r.k.show.padTo(maxKeySize, " ").mkString}  :  ${r.v.show}"
        }

      paddedList.mkString("\n")
    }
}
