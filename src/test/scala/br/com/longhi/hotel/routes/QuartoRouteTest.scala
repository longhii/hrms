package br.com.longhi.hotel.routes

import br.com.longhi.hotel.databases.SlickDatabase._
import br.com.longhi.hotel.{ControleReservasSwagger, Tables}
import org.junit.Assert.assertEquals
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraFunSuite
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class QuartoRouteTest extends ScalatraFunSuite with BeforeAndAfterEach {
  override def header = response.header

  private implicit val swagger = new ControleReservasSwagger

  addServlet(new QuartoRoute, "/quartos")

  override def beforeEach(): Unit = {
    super.beforeEach()

    Await.result(db.run(Tables.quartos.schema.create), 2.seconds)
  }

  override def afterEach(): Unit = {
    super.afterEach()

    Await.result(db.run(Tables.quartos.schema.drop), 2.seconds)
  }

  test("Validar criação de quarto com sucesso") {
    val json =
      """
        |{
        |    "numero" : 913,
        |    "tipo" : "Standard",
        |    "capacidade" : 2
        |}
        |""".stripMargin

    post("/quartos", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(201, status)
      assertEquals("{\"quartoId\":1}", body)
    }
  }

  test("Validar erro identificador duplicado ao criar quarto") {
    Await.result(db.run(Tables.quartos += Tables.Quarto(numero = 913, tipo = "Standard", capacidade = 2)), 2.seconds)

    val json =
      """
        |{
        |    "numero" : 913,
        |    "tipo" : "Standard",
        |    "capacidade" : 2
        |}
        |""".stripMargin

    post("/quartos", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(409, status)
      assertEquals("Um quarto com o mesmo identificador único já existe.", body)
    }
  }

  test("Validar erro corpo requisição inválido ao criar quarto") {
    val json =
      """
        |{
        |    "numero" : 913,
        |    "type" : "Standard",
        |    "capacidade" : 2
        |}
        |""".stripMargin

    post("/quartos", body = json, headers = Map("Content-Type" -> "application/json")) {
      assertEquals(400, status)
      assertEquals("Corpo da requisição inválido.", body)
    }
  }

  test("Validar consulta de quartos") {
    Await.result(db.run(Tables.quartos ++= Seq(
      Tables.Quarto(numero = 301, tipo = "Standard", capacidade = 2),
      Tables.Quarto(numero = 302, tipo = "Standard", capacidade = 2),
      Tables.Quarto(numero = 303, tipo = "Triple Master", capacidade = 3),
    )), 2.seconds)

    get("/quartos", headers = Map("Content-Type" -> "application/json")) {
      assertEquals(200, status)
      assertEquals("[{\"id\":1,\"numero\":301,\"tipo\":\"Standard\",\"capacidade\":2},{\"id\":2,\"numero\":302,\"tipo\":\"Standard\",\"capacidade\":2},{\"id\":3,\"numero\":303,\"tipo\":\"Triple Master\",\"capacidade\":3}]", body)
    }
  }

  test("Validar remoção de quarto com sucesso") {
    Await.result(db.run(Tables.quartos += Tables.Quarto(numero = 913, tipo = "Standard", capacidade = 2)), 2.seconds)

    delete("/quartos/1") {
      assertEquals(204, status)
      assertEquals("", body)
    }

    val quartos = Await.result(db.run(Tables.quartos.result), 2.seconds)
    assertEquals(0, quartos.size)
  }

  test("Validar erro para quarto não localizado ao remover um quarto") {
    delete("/quartos/1") {
      assertEquals(404, status)
      assertEquals("Quarto com id 1 não encontrado.", body)
    }
  }
}
