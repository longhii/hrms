package br.com.longhi.hotel.routes

import br.com.longhi.hotel.Tables.{hospedes, quartos, reservas, reservasQuartos}
import br.com.longhi.hotel.{ControleReservasSwagger, Tables}
import br.com.longhi.hotel.databases.SlickDatabase._
import org.junit.Assert.assertEquals
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraFunSuite
import slick.jdbc.H2Profile.api._

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

class FluxoRouteTest extends ScalatraFunSuite with BeforeAndAfterEach {

  override def header = response.header

  private implicit val swagger = new ControleReservasSwagger

  addServlet(new FluxoRoute, "/fluxo")

  private lazy val schema = Seq(hospedes, reservasQuartos, reservas, quartos)

  override def beforeEach(): Unit = {
    super.beforeEach()

    Await.result(db.run(schema
      .map(_.schema)
      .reduce(_ ++ _)
      .create), 2.seconds)
  }

  override def afterEach(): Unit = {
    super.afterEach()

    Await.result(db.run(schema
      .map(_.schema)
      .reduce(_ ++ _)
      .drop), 2.seconds)
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  private def inserirDependenciasTest(dataCheckIn: Option[LocalDateTime] = None,
                                      disponivelAs: Option[LocalDateTime] = None) = {
    Await.result(db.run(DBIO.seq(
      Tables.hospedes += Tables.Hospede(id = 1, cpf = "000.100.101-01", nome = "John Doe"),
      Tables.quartos += Tables.Quarto(id = 1, numero = 301, tipo = "Standard", capacidade = 2, disponivelAs = disponivelAs),
      Tables.reservas += Tables.Reserva(id = 1, hospedeId = 1),
      Tables.reservasQuartos += Tables.ReservaQuartos(
        reservaId = 1,
        quartoId = 1,
        previsaoCheckIn = LocalDateTime.of(2024, 8, 20, 20, 30),
        previsaoCheckOut = LocalDate.of(2024, 8, 21),
        dataCheckIn = dataCheckIn,
        dataCheckOut = None)
    )), Duration.Inf)
  }

  test("Validar check-in efetuado com sucesso") {
    inserirDependenciasTest()

    val json =
      """
        |{
        | "reservaId": 1,
        | "quartoId": 1,
        | "dataHorario": "2024-08-20T20:40:00"
        |}
        |""".stripMargin

    post("/fluxo/check-in", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(200, status)
      assertEquals("", body)
    }

    val reservasQuartos = Await.result(db.run(Tables.reservasQuartos.result), 2.seconds)
    assertEquals(1, reservasQuartos.length)
    assertEquals(Some(LocalDateTime.of(2024, 8, 20, 20, 40)), reservasQuartos.head.dataCheckIn)
  }

  test("Validar erro para corpo de requisição inválido ao efetuar check-in") {
    val json =
      """
        |{
        | "reservaId": 1,
        | "quartId": 1,
        | "dataHorario": "2024-08-20T20:40:00"
        |}
        |""".stripMargin

    post("/fluxo/check-in", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(400, status)
      assertEquals("Corpo da requisição inválido.", body)
    }
  }

  test("Validar erro para quarto ou reserva não encontrada ao efetuar check-in") {
    inserirDependenciasTest()

    val json =
      """
        |{
        | "reservaId": 1,
        | "quartoId": 2,
        | "dataHorario": "2024-08-20T20:40:00"
        |}
        |""".stripMargin

    post("/fluxo/check-in", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(404, status)
      assertEquals("Quarto ou reserva não localizada.", body)
    }

  }

  test("Validar check-out efetuado com sucesso") {
    inserirDependenciasTest(dataCheckIn = Some(LocalDateTime.of(2024, 8, 20, 20, 40)))

    val json =
      """
        |{
        | "reservaId": 1,
        | "quartoId": 1,
        | "dataHorario": "2024-08-21T08:30:00"
        |}
        |""".stripMargin

    post("/fluxo/check-out", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(200, status)
      assertEquals("", body)
    }

    val reservasQuartos = Await.result(db.run(Tables.reservasQuartos.result), 2.seconds)
    assertEquals(1, reservasQuartos.length)
    assertEquals(Some(LocalDateTime.of(2024, 8, 21, 8, 30)), reservasQuartos.head.dataCheckOut)
  }

  test("Validar erro para corpo de requisição inválido ao efetuar check-out") {
    val json =
      """
        |{
        | "reservId": 1,
        | "quartoId": 1,
        | "dataHorario": "2024-08-21T08:30:00"
        |}
        |""".stripMargin

    post("/fluxo/check-out", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(400, status)
      assertEquals("Corpo da requisição inválido.", body)
    }
  }

  test("Validar erro para quarto ou reserva não encontrada ao efetuar check-out") {
    inserirDependenciasTest(dataCheckIn = Some(LocalDateTime.of(2024, 8, 20, 20, 40)))

    val json =
      """
        |{
        | "reservaId": 2,
        | "quartoId": 1,
        | "dataHorario": "2024-08-21T08:30:00"
        |}
        |""".stripMargin

    post("/fluxo/check-out", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(404, status)
      assertEquals("Quarto ou reserva não localizada.", body)
    }
  }

  test("Validar erro para data inválida ao efetuar check-out") {
    inserirDependenciasTest(dataCheckIn = Some(LocalDateTime.of(2024, 8, 20, 20, 40)))

    val json =
      """
        |{
        | "reservaId": 1,
        | "quartoId": 1,
        | "dataHorario": "2024-08-20T08:30:00"
        |}
        |""".stripMargin

    post("/fluxo/check-out", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(409, status)
      assertEquals("A data de check-out deve ser posterior à data de check-in.", body)
    }
  }

  test("Validar erro para check-out sem check-in") {
    inserirDependenciasTest()

    val json =
      """
        |{
        | "reservaId": 1,
        | "quartoId": 1,
        | "dataHorario": "2024-08-21T08:30:00"
        |}
        |""".stripMargin

    post("/fluxo/check-out", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(409, status)
      assertEquals("A data de check-out deve ser posterior à data de check-in.", body)
    }
  }

  test("Validar reserva efetuada com sucesso") {
    Await.result(db.run(DBIO.seq(
      Tables.hospedes += Tables.Hospede(id = 1, cpf = "000.100.101-01", nome = "John Doe"),
      Tables.quartos += Tables.Quarto(id = 1, numero = 301, tipo = "Standard", capacidade = 2))), Duration.Inf)

    val json =
      """
        |{
        | "hospedeId": 1,
        | "dataCheckIn": "2024-08-20T21:30:00",
        | "dataCheckOut": "2024-08-22",
        | "quartos": [1]
        |}
        |""".stripMargin

    post("/fluxo/reserva", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(201, status)
      assertEquals("{\"reservaId\":1}", body)
    }

    val reservas = Await.result(db.run(Tables.reservas.result), 2.seconds)
    assertEquals(1, reservas.length)
    assertEquals(1, reservas.head.hospedeId)

    val reservasQuartos = Await.result(db.run(Tables.reservasQuartos.result), 2.seconds)
    assertEquals(1, reservasQuartos.length)
    assertEquals(1, reservasQuartos.head.reservaId)
    assertEquals(1, reservasQuartos.head.quartoId)
    assertEquals(None, reservasQuartos.head.dataCheckIn)
    assertEquals(None, reservasQuartos.head.dataCheckOut)
    assertEquals(LocalDateTime.of(2024, 8, 20, 21, 30), reservasQuartos.head.previsaoCheckIn)
    assertEquals(LocalDate.of(2024, 8, 22), reservasQuartos.head.previsaoCheckOut)

    val quarto = Await.result(db.run(Tables.quartos.result), 2.seconds)
    assertEquals(Some(LocalDateTime.of(2024, 8, 22, 17, 0)), quarto.head.disponivelAs)
  }

  test("Validar erro corpo de requisição inválido ao efetuar reserva") {
    val json =
      """
        |{
        | "hospedeId": 1,
        | "dataCheckIn": "2024-08-20T21:30:00",
        | "quartos": [1]
        |}
        |""".stripMargin

    post("/fluxo/reserva", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(400, status)
      assertEquals("Corpo da requisição inválido.", body)
    }
  }

  test("Validar erro para duplicidade ao efetuar reserva") {
    inserirDependenciasTest(disponivelAs = Some(LocalDateTime.of(2024, 8, 21, 17, 0)))

    val json =
      """
        |{
        | "hospedeId": 1,
        | "dataCheckIn": "2024-08-20T22:00:00",
        | "dataCheckOut": "2024-08-21",
        | "quartos": [1]
        |}
        |""".stripMargin

    post("/fluxo/reserva", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(409, status)
      assertEquals("Já existe uma reserva para o(s) quarto(s) selecionado(s) dentro do período especificado.", body)
    }
  }

  test("Validar erro para para que ao efetuar a reserva a data check-in seja inferior a data check-out") {
    val json =
      """
        |{
        | "hospedeId": 1,
        | "dataCheckIn": "2024-08-21T22:00:00",
        | "dataCheckOut": "2024-08-21",
        | "quartos": [1]
        |}
        |""".stripMargin

    post("/fluxo/reserva", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(409, status)
      assertEquals("A data de check-in deve ser anterior a data de check-out.", body)
    }
  }

  test("Validar erro para hospede não cadastrado ao efetuar reserva.") {
    val json =
      """
        |{
        | "hospedeId": 5,
        | "dataCheckIn": "2024-09-03T22:00:00",
        | "dataCheckOut": "2024-09-04",
        | "quartos": [1]
        |}
        |""".stripMargin

    post("/fluxo/reserva", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(500, status)
      assertEquals("Erro interno no servidor. Por favor, entre em contato com o suporte.", body)
    }
  }

  test("Validar consulta taxa de ocupação") {
    Await.result(db.run(DBIO.seq(
      Tables.hospedes += Tables.Hospede(id = 1, cpf = "000.100.101-01", nome = "John Doe"),
      Tables.quartos ++= Seq(
        Tables.Quarto(id = 1, numero = 301, tipo = "Standard", capacidade = 2, disponivelAs = Some(LocalDateTime.of(2024, 8, 21, 17, 0))),
        Tables.Quarto(id = 1, numero = 302, tipo = "Standard", capacidade = 2),
        Tables.Quarto(id = 1, numero = 303, tipo = "Standard", capacidade = 2),
        Tables.Quarto(id = 1, numero = 304, tipo = "Standard", capacidade = 2)),
      Tables.reservas += Tables.Reserva(id = 1, hospedeId = 1),
      Tables.reservasQuartos += Tables.ReservaQuartos(
        reservaId = 1,
        quartoId = 1,
        previsaoCheckIn = LocalDateTime.of(2024, 8, 20, 20, 30),
        previsaoCheckOut = LocalDate.of(2024, 8, 21),
        dataCheckIn = None,
        dataCheckOut = None)
    )), Duration.Inf)

    val dataParam = "2024-08-20T21:00"

    get(s"/fluxo/taxa-ocupacao?data=$dataParam") {
      assertEquals(200, status)
      assertEquals("25.0", body)
    }
  }

  test("Validar erro para consulta taxa de ocupação sem data fornecida") {
    get("/fluxo/taxa-ocupacao") {
      assertEquals(400, status)
      assertEquals("Parâmetro: 'data' não fornecido.", body)
    }
  }

  test("Validar erro para data com formato inválido ao consultar taxa de ocupação") {
    val dataParam = "2024-08-20"

    get(s"/fluxo/taxa-ocupacao?data=$dataParam") {
      assertEquals(400, status)
      assertEquals("Formato de data inválido. Utilize o formato: '2007-12-03T10:15:30'.", body)
    }
  }
}
