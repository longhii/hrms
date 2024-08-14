package br.com.longhi.hotel.routes

import br.com.longhi.hotel.Tables
import br.com.longhi.hotel.Tables.{Quarto, quartos}
import org.json4s.JsonDSL._
import org.scalatra._
import org.scalatra.swagger._
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._

import scala.util.{Failure, Success, Try}

class QuartoRoute(val db: Database)(implicit val swagger: Swagger) extends ScalatraServlet
  with Routes
  with SwaggerSupport {

  protected val applicationDescription: String = "API para gerenciamento de quartos"

  private lazy val log = LoggerFactory.getLogger(classOf[QuartoRoute])

  val postQuarto =
    (apiOperation[Unit]("createQuarto")
      summary "Cria um novo quarto"
      description "Este endpoint permite a criação de um novo quarto."
      parameter bodyParam[Quarto]("quarto").description("Objeto Quarto a ser criado")
      responseMessage ResponseMessage(201, "Quarto criado com sucesso")
      responseMessage ResponseMessage(400, "Corpo da requisição inválido")
      responseMessage ResponseMessage(409, "Um quarto com o mesmo identificador único já existe"))

  post("/", operation(postQuarto)) {
    log.info("Criando um quarto...")

    Try(parsedBody.extract[Quarto]) match {
      case Success(quarto) =>
        val insertAction = (Tables.quartos returning Tables.quartos.map(_.id)) += quarto

        val result = db.run(insertAction).map { id =>
          Right(id)
        }.recover {
          case _ => Left("Um quarto com o mesmo identificador único já existe.")
        }

        result.map {
          case Right(id) => Created(compact(render("quartoId" -> id)))
          case Left(errorMsg) => Conflict(errorMsg)
        }
      case Failure(_) => BadRequest("Corpo da requisição inválido.")
    }
  }

  // Documentação do endpoint GET /
  val getQuartos =
    (apiOperation[List[Quarto]]("getQuartos")
      summary "Retorna a lista de quartos"
      description "Este endpoint retorna a lista completa de quartos cadastrados."
      responseMessage ResponseMessage(500, "Erro inesperado"))

  get("/", operation(getQuartos)) {
    log.info("Consultando quartos...")

    db.run(Tables.quartos.result)
      .map(rs => Ok(rs))
      .recover { case e => InternalServerError(s"Um erro inesperado ocorreu: $e") }
  }

  val deleteQuarto =
    (apiOperation[Unit]("deleteQuarto")
      summary "Deleta um quarto"
      description "Este endpoint permite deletar um quarto pelo seu ID. Cuidado, remover um quarto significa remover ele e todos os dados vinculados a ele."
      parameter pathParam[Long]("id").description("ID do quarto a ser deletado")
      responseMessage ResponseMessage(204, "Quarto deletado com sucesso")
      responseMessage ResponseMessage(404, "Quarto não encontrado"))

  delete("/:id",  operation(deleteQuarto)) {
    log.info("Deletando um quarto...")

    val roomId = params("id").toLong
    db.run(quartos.filter(_.id === roomId).delete).map { rows =>
      if (rows > 0) {
        NoContent()
      } else {
        NotFound(s"Quarto com id $roomId não encontrado.")
      }
    }
  }

}
