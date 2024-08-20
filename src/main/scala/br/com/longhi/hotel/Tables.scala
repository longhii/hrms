package br.com.longhi.hotel

import slick.jdbc.H2Profile.api._
import slick.lifted.{TableQuery, Tag}

import java.time.{LocalDate, LocalDateTime}

object Tables {

  case class Quarto(id: Long = 0L, numero: Int, tipo: String, capacidade: Int, disponivelAs: Option[LocalDateTime] = None)

  class QuartoTable(tag: Tag) extends Table[Quarto](tag, "quartos") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def numero = column[Int]("numero", O.Unique)

    def tipo = column[String]("tipo")

    def capacidade = column[Int]("capacidade")

    def disponivelAs = column[Option[LocalDateTime]]("disponivel_as")

    def * = (id, numero, tipo, capacidade, disponivelAs).mapTo[Quarto]
  }

  lazy val quartos = TableQuery[QuartoTable]

  case class Hospede(id: Long = 0L, cpf: String, nome: String)

  class HospedeTable(tag: Tag) extends Table[Hospede](tag, "hospedes") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def cpf = column[String]("cpf", O.Unique)

    def nome = column[String]("nome")

    def * = (id, cpf, nome).mapTo[Hospede]
  }

  lazy val hospedes = TableQuery[HospedeTable]

  case class Reserva(id: Long = 0L,
                     hospedeId: Long)

  class ReservaTable(tag: Tag) extends Table[Reserva](tag, "reservas") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def hospedeId = column[Long]("hospede_id")

    def hospede =
      foreignKey("hospede_fk", hospedeId, hospedes)(_.id, onDelete = ForeignKeyAction.NoAction)

    def * = (
      id,
      hospedeId).mapTo[Reserva]

  }

  lazy val reservas = TableQuery[ReservaTable]

  case class ReservaQuartos(reservaId: Long, quartoId: Long,
                            previsaoCheckIn: LocalDateTime,
                            previsaoCheckOut: LocalDate,
                            dataCheckIn: Option[LocalDateTime] = None,
                            dataCheckOut: Option[LocalDateTime] = None)

  class ReservasQuartosTable(tag: Tag) extends Table[ReservaQuartos](tag, "reservas_quartos") {
    def reservaId = column[Long]("reserva_id")

    def quartoId = column[Long]("quarto_id")

    def previsaoCheckIn = column[LocalDateTime]("previsao_check_in")

    def previsaoCheckOut = column[LocalDate]("previsao_check_out")

    def dataCheckIn = column[Option[LocalDateTime]]("data_check_in")

    def dataCheckOut = column[Option[LocalDateTime]]("data_check_out")

    def reserva =
      foreignKey("reserva_fk", reservaId, reservas)(_.id, onDelete = ForeignKeyAction.Cascade)

    def quarto =
      foreignKey("quarto_fk", quartoId, quartos)(_.id, onDelete = ForeignKeyAction.Cascade)

    def pk = primaryKey("pk_reserva_quarto", (reservaId, quartoId))

    def * = (reservaId, quartoId,
      previsaoCheckIn,
      previsaoCheckOut,
      dataCheckIn,
      dataCheckOut).mapTo[ReservaQuartos]
  }

  lazy val reservasQuartos = TableQuery[ReservasQuartosTable]

  def criarSchema() =
    (quartos.schema ++ hospedes.schema ++ reservas.schema ++ reservasQuartos.schema).create

  def popularBanco() =
    DBIO.seq(
      Tables.quartos += Quarto(numero = 301, tipo = "Standard", capacidade = 2),
      Tables.quartos += Quarto(numero = 302, tipo = "Standard", capacidade = 2),
      Tables.quartos += Quarto(numero = 303, tipo = "Standard", capacidade = 2),
      Tables.quartos += Quarto(numero = 304, tipo = "Triple Standard", capacidade = 3),
      Tables.quartos += Quarto(numero = 305, tipo = "Master", capacidade = 2),
      Tables.quartos += Quarto(numero = 306, tipo = "Master", capacidade = 2),
      Tables.quartos += Quarto(numero = 307, tipo = "Master", capacidade = 2),
      Tables.quartos += Quarto(numero = 308, tipo = "Triple Master", capacidade = 3),
      Tables.hospedes += Hospede(cpf = "000.000.000-00", nome = "John Doe")
    )

}
