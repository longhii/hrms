package br.com.longhi.hotel.routes

import br.com.longhi.hotel.serializers.{LocalDateSerializer, LocalDateTimeSerializer}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import slick.jdbc.H2Profile.api._

trait Routes extends ScalatraBase with FutureSupport with JacksonJsonSupport {
  def db: Database

  protected override implicit lazy val jsonFormats: Formats = DefaultFormats + LocalDateSerializer + LocalDateTimeSerializer

  protected implicit override def executor = scala.concurrent.ExecutionContext.Implicits.global

  before() {
    contentType = "application/json"
  }

}
