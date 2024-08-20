package br.com.longhi.hotel.routes

import br.com.longhi.hotel.{ControleReservasSwagger, Tables}
import org.junit.Assert.assertEquals
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest._
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class HospedeRouteTest extends ScalatraFunSuite with BeforeAndAfterEach {
  override def header = response.header

  private implicit val swagger = new ControleReservasSwagger

  private val db = Database
    .forURL(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      executor = AsyncExecutor("test", numThreads = 10, queueSize = 1000),
      driver = "org.h2.Driver")

  addServlet(new HospedeRoute(db), "/hospedes")

  override def beforeAll(): Unit = {
    super.beforeAll()
    Await.result(db.run(Tables.hospedes.schema.create), 2.seconds)
  }

  override def afterAll(): Unit = {
    Await.result(db.run(Tables.hospedes.schema.drop), 2.seconds)
    db.close()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    Await.result(db.run(Tables.hospedes.delete), 2.seconds)
  }

  test("Validar criação de hóspede com sucesso.") {
    val json =
      """
        |{
        | "nome": "John Doe",
        | "cpf": "000.100.101-01"
        |}
        |""".stripMargin


    post("/hospedes", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(201, status)
      assertEquals("{\"hospedeId\":1}", body)
    }

    val hospedes = Await.result(db.run(Tables.hospedes.result), 2.seconds)
    assertEquals(1, hospedes.length)
    assertEquals("John Doe", hospedes.head.nome)
  }

  test("Validar criação de hóspede, CPF já cadastrado") {
    val json =
      """
        |{
        | "nome": "John Doe",
        | "cpf": "000.100.101-01"
        |}
        |""".stripMargin

    Await.result(db.run(Tables.hospedes += Tables.Hospede(cpf = "000.100.101-01", nome = "John Doe")), 2.seconds)

    post("/hospedes", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(409, status)
      assertEquals("Um hóspede com o mesmo CPF já existe.", body)
    }

    val hospedes = Await.result(db.run(Tables.hospedes.result), 2.seconds)
    assertEquals(1, hospedes.length)
    assertEquals("John Doe", hospedes.head.nome)
  }

  test("Validar criação de hóspede, corpo da requisição inválido") {
    val json =
      """
        |{
        | "any": "John Doe",
        | "cpf": "000.100.101-01"
        |}
        |""".stripMargin

    post("/hospedes", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(400, status)
      assertEquals("Corpo da requisição inválido.", body)
    }
  }
}
