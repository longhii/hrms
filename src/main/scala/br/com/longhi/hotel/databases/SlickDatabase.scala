package br.com.longhi.hotel.databases

import br.com.longhi.hotel.Tables
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

object SlickDatabase {

  lazy val db = Database
    .forURL(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      executor = AsyncExecutor("test", numThreads = 10, queueSize = 1000),
      driver = "org.h2.Driver")

  def init(): Unit = {
    Await.result(db.run(Tables.criarSchema()), 7.seconds)
    Await.result(db.run(Tables.popularBanco()), Duration.Inf)
  }

}
