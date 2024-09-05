package br.com.longhi.hotel.routes

import br.com.longhi.hotel.exceptions.ParameterNotFoundException
import br.com.longhi.hotel.serializers.{LocalDateSerializer, LocalDateTimeSerializer}
import org.json4s.{DefaultFormats, Formats, MappingException}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.slf4j.LoggerFactory

trait Routes extends ScalatraServlet
  with FutureSupport
  with JacksonJsonSupport
  with ScalatraBase {

  private lazy val log = LoggerFactory.getLogger(classOf[Routes])

  protected override implicit lazy val jsonFormats: Formats = DefaultFormats + LocalDateSerializer + LocalDateTimeSerializer

  protected implicit override def executor = scala.concurrent.ExecutionContext.Implicits.global

  before() {
    contentType = "application/json"
  }

  error {

    case e: ParameterNotFoundException =>
      log.error("Erro, parâmetro não localizado: " + e.getMessage, e)
      response.setStatus(400)
      e.getMessage

    case e: MappingException =>
      log.error("Erro, corpo da requisição inválido: " + e.getMessage, e)
      response.setStatus(400)
      "Corpo da requisição inválido."

    case e: Exception =>
      log.error("Erro interno: " + e.getMessage, e)
      response.setStatus(500)
      "Erro interno no servidor. Por favor, entre em contato com o suporte."
  }

}
