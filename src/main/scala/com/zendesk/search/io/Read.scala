package com.zendesk.search.io

import java.nio.file.Paths

import cats.effect.IO
import fs2.io.file.Files
import fs2.{ text, Stream }
import io.circe.Json
import io.circe.fs2.stringArrayParser

object Read {
  def fromJsonFile[A](filePath: String)(f: Json => Either[String, A]): Stream[IO, A] =
    toJson(readFile(filePath)).flatMap { r =>
      f(r) match {
        case Left(value)  =>
          Stream.raiseError[IO](new RuntimeException(s"Failed to read the entity from ${filePath}. Error message: ${value}"))
        case Right(value) => Stream.eval(IO.pure(value))
      }
    }

  def readFile(filePath: String): Stream[IO, String] =
    Files[IO]
      .readAll(
        Paths.get(filePath),
        4096
      )
      .through(text.utf8Decode)
      .through(text.lines)

  def toJson[A](str: Stream[IO, String]): Stream[IO, Json] =
    str.through(stringArrayParser[IO])
}
