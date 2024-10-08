package br.com.longhi.hotel.routes

import br.com.longhi.hotel.DTO
import br.com.longhi.hotel.exceptions.ParameterNotFoundException
import br.com.longhi.hotel.services.FluxoService
import org.json4s.JsonDSL._
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.slf4j.LoggerFactory

class FluxoRoute(implicit val swagger: Swagger)
  extends Routes
    with SwaggerSupport {

  protected val applicationDescription: String = "API para gerenciamento de fluxo"

  private lazy val service = new FluxoService

  private lazy val log = LoggerFactory.getLogger(classOf[FluxoRoute])

  val postCheckIn =
    apiOperation[Unit]("checkIn")
      .summary("Registra o check-in de um hóspede")
      .description("Este endpoint permite registrar o check-in de um hóspede com base nas informações fornecidas.")
      .parameter(bodyParam[DTO.FluxoEntradaSaida]("checkInDTO").description("Dados necessários para o check-in"))
      .responseMessage(ResponseMessage(200, "Check-in realizado com sucesso"))
      .responseMessage(ResponseMessage(400, "Corpo da requisição inválido"))
      .responseMessage(ResponseMessage(404, "Quarto ou reserva não localizada."))

  post("/check-in", operation(postCheckIn)) {
    service.efetuarCheckIn(parsedBody.extract[DTO.FluxoEntradaSaida])
  }

  val postCheckOut =
    apiOperation[Unit]("checkOut")
      .summary("Registra o check-out de um hóspede")
      .description("Este endpoint permite registrar o check-out de um hóspede com base nas informações fornecidas.")
      .parameter(bodyParam[DTO.FluxoEntradaSaida]("checkOutDTO").description("Dados necessários para o check-out"))
      .responseMessage(ResponseMessage(200, "Check-out realizado com sucesso"))
      .responseMessage(ResponseMessage(400, "Corpo da requisição inválido"))
      .responseMessage(ResponseMessage(409, "A data de check-out deve ser posterior à data de check-in."))
      .responseMessage(ResponseMessage(404, "Quarto ou reserva não localizada."))
      .responseMessage(ResponseMessage(500, "Erro interno no servidor."))

  post("/check-out", operation(postCheckOut)) {
    service.efetuarCheckOut(parsedBody.extract[DTO.FluxoEntradaSaida])
      .map(_ => Ok())
      .recover {
        case e: NoSuchElementException => NotFound(e.getMessage)
        case e: IllegalArgumentException => Conflict(e.getMessage)
      }
  }

  val postReserva =
    apiOperation[Unit]("createReserva")
      .summary("Cria uma nova reserva")
      .description("Este endpoint permite criar uma nova reserva com base nas informações fornecidas.")
      .parameter(bodyParam[DTO.Reserva]("reserva").description("Dados necessários para criar a reserva"))
      .responseMessage(ResponseMessage(201, "Reserva criada com sucesso"))
      .responseMessage(ResponseMessage(400, "Corpo da requisição inválido"))
      .responseMessage(ResponseMessage(409, "Já existe uma reserva para o(s) quarto(s) selecionado(s) dentro do período especificado, ou data check-in anterior a check-out."))

  post("/reserva", operation(postReserva)) {
    service
      .efetuarReserva(parsedBody.extract[DTO.Reserva])
      .map { id =>
        Created(compact(render("reservaId" -> id)))
      }.recover {
        case e: IllegalArgumentException =>
          log.info("Erro ao efetuar reserva: " + e.getMessage, e)
          Conflict(e.getMessage)
      }
  }

  val getTaxaOcupacao =
    apiOperation[Double]("getTaxaOcupacao")
      .summary("Obtém a taxa de ocupação para uma data específica")
      .description("Este endpoint calcula a taxa de ocupação com base na data fornecida.")
      .parameter(queryParam[String]("data").description("Data para cálculo da taxa de ocupação no formato '2007-12-03T10:15:30'"))
      .responseMessage(ResponseMessage(200, "Taxa de ocupação retornada com sucesso"))
      .responseMessage(ResponseMessage(400, "Formato de data inválido ou parâmetro data não encontrado"))
      .responseMessage(ResponseMessage(500, "Erro interno do servidor"))

  get("/taxa-ocupacao", operation(getTaxaOcupacao)) {
    val dataStr = params
      .getOrElse("data", throw new ParameterNotFoundException("Parâmetro: 'data' não fornecido."))

    service
      .verificarTaxaOcupacao(dataStr)
      .map(tx => Ok(tx))
      .recover { case e: IllegalArgumentException => BadRequest(e.getMessage) }
  }

}
