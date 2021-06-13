package com.zendesk.search.io

import cats.effect.IO

trait ConsoleIO {
  def putStrLn(message: String): IO[Unit]
  def readLine: IO[String]
}

object ConsoleIO {
  def from(put: String => IO[Unit], read: IO[String]): ConsoleIO =
    new ConsoleIO {
      override def putStrLn(message: String): IO[Unit] = put(message)
      override def readLine: IO[String]                = read
    }

  def live: ConsoleIO =
    from(str => IO(println(str)), IO.delay(scala.io.StdIn.readLine()))
}
