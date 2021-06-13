package com.zendesk.search.support

import cats.effect.IO
import cats.syntax.either._

trait IOSyntax {
  implicit class IOError[A](io: IO[A]) {
    def withMessageOnError(message: => String): IO[A] =
      io.attempt.flatMap(either => IO.fromEither(either.leftMap(r => new RuntimeException(s"${message}: ${r}"))))
  }
}
