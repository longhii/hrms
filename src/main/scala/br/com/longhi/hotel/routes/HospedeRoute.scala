package br.com.longhi.hotel.routes

import br.com.longhi.hotel.Tables
import br.com.longhi.hotel.Tables.Hospede
import br.com.longhi.hotel.databases.SlickDatabase._
import org.json4s.JsonDSL._
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._

import scala.util.{Failure, Success, Try}

class HospedeRoute(implicit val swagger: Swagger)
  extends Routes
  with SwaggerSupport {

  protected val applicationDescription: String = "API para gerenciamento de hóspedes"

  private lazy val log = LoggerFactory.getLogger(classOf[HospedeRoute])

  val getHospedes =
    (apiOperation[Unit]("getHospedes")
      summary "Retorna a lista de hóspedes"
      description "Este endpoint retorna a lista completa de hóspedes cadastrados."
      responseMessage ResponseMessage(501, "Erro inesperado."))

  get("/", operation(getHospedes)) {
    log.info("Consultando hóspedes...")

    db.run(Tables.hospedes.result)
      .map(rs => Ok(rs))
      .recover { case e => InternalServerError(s"Um erro inesperado ocorreu: $e") }
  }

  val postHospede =
    (apiOperation[Unit]("createHospede")
      summary "Cria um novo hóspede"
      description "Este endpoint permite a criação de um novo hóspede."
      parameter bodyParam[Hospede]("hospede").description("Objeto Hospede a ser criado")
      responseMessage ResponseMessage(201, "Hóspede criado com sucesso")
      responseMessage ResponseMessage(400, "Corpo da requisição inválido")
      responseMessage ResponseMessage(409, "Um hóspede com o mesmo CPF já existe."))

  post("/", operation(postHospede)) {
    log.info("Criando hóspede...")

    Try(parsedBody.extract[Hospede]) match {
      case Success(hospede) =>
        val insertAction = (Tables.hospedes returning Tables.hospedes.map(_.id)) += hospede

        val result = db.run(insertAction).map { id =>
          Right(id)
        }.recover {
          case _ => Left("Um hóspede com o mesmo CPF já existe.")
        }

        result.map {
          case Right(id) => Created(compact(render("hospedeId" -> id)))
          case Left(errorMsg) => Conflict(errorMsg)
        }
      case Failure(_) => BadRequest("Corpo da requisição inválido.")
    }
  }

}
