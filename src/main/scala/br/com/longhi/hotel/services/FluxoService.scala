package br.com.longhi.hotel.services

import br.com.longhi.hotel.DTO.Reserva
import br.com.longhi.hotel.{DTO, Tables}
import org.scalatra.{BadRequest, NoContent, NotFound, Ok}
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext
import scala.math.BigDecimal.RoundingMode

class FluxoService(db: Database)(implicit val executor: ExecutionContext) {

  private lazy val log = LoggerFactory.getLogger(classOf[FluxoService])

  def efetuarReserva(reservaDTO: Reserva) = {
    log.info("Efetuando a reserva...")

    val validacaoDataCheckInCheckOut =
      if (reservaDTO.dataCheckIn.toLocalDate.isAfter(reservaDTO.dataCheckOut) || reservaDTO.dataCheckIn.toLocalDate.equals(reservaDTO.dataCheckOut)) {
        DBIO.failed(new Exception("A data de check-in deve ser anterior a data de check-out."))
      } else {
        DBIO.successful()
      }

    val action = for {
      _ <- validacaoDataCheckInCheckOut
      exists <- Tables.reservasQuartos
        .join(Tables.quartos)
        .on(_.quartoId === _.id)
        .filter { case (rq, q) =>
          rq.quartoId.inSet(reservaDTO.quartos) &&
            q.disponivelAs > reservaDTO.dataCheckIn
        }
        .exists
        .result

      result <- if (exists) {
        DBIO.failed(new Exception("Já existe uma reserva para o(s) quarto(s) selecionado(s) dentro do período especificado."))
      } else {
        val insertReservaAction =
          (Tables.reservas returning Tables.reservas.map(_.id)) += Tables.Reserva(hospedeId = reservaDTO.hospedeId)

        insertReservaAction.flatMap { reservaId =>
          val insertReservasQuartosActions = reservaDTO.quartos.map { quartoId =>
            val insertAction = Tables.reservasQuartos += Tables.ReservaQuartos(reservaId, quartoId, previsaoCheckIn = reservaDTO.dataCheckIn,
              previsaoCheckOut = reservaDTO.dataCheckOut,
              dataCheckIn = None,
              dataCheckOut = None)

            val updateAction = Tables.quartos
              .filter(q => q.id === quartoId)
              .map(q => q.disponivelAs)
              .update(Some(reservaDTO.dataCheckOut.atTime(17, 0)))

            insertAction.flatMap(_ => updateAction)
          }
          DBIO.sequence(insertReservasQuartosActions).map(_ => reservaId)
        }
      }
    } yield result

    db.run(action.transactionally)
  }

  def efetuarCheckIn(checkInDTO: DTO.FluxoEntradaSaida) = {
    log.info("Efetuando check-in...")

    val updateCheckInAction = Tables.reservasQuartos
      .filter(rq => rq.quartoId === checkInDTO.quartoId && rq.reservaId === checkInDTO.reservaId)
      .map(rq => rq.dataCheckIn)
      .update(Some(checkInDTO.dataHorario))

    val checkInAction = updateCheckInAction.flatMap { rowsAffected =>
      if (rowsAffected > 0) {
        DBIO.successful(Ok())
      } else {
        DBIO.successful(NotFound("Quarto ou reserva não localizada."))
      }
    }

    db.run(checkInAction.transactionally)
  }

  /*
     Ao realizar check-out é preciso:
      - verificar se existe outra reserva para o mesmo quarto com check-in previsto posterior ao que está sendo realizado check-out
      - caso sim, não será atualizado o horário disponível do quarto (disponivelAs)
      - caso não, será atualizado o horário disponível do quarto, entende-se que o hóspede saiu de forma
        antecipada ao horário limite 13h + 4h (limpeza) e que a limpeza do mesmo será adiantada.
   */
  def efetuarCheckOut(checkOutDTO: DTO.FluxoEntradaSaida) = {
    log.info("Efetuando check-out...")

    val reservaQuartoCheckOut = Tables.reservasQuartos
      .filter(rq => rq.quartoId === checkOutDTO.quartoId && rq.reservaId === checkOutDTO.reservaId)
      .result
      .headOption

    val oq = reservaQuartoCheckOut.flatMap { case Some(rqc) =>
      if (rqc.dataCheckIn.isEmpty
        || checkOutDTO.dataHorario.isBefore(rqc.dataCheckIn.get)
        || checkOutDTO.dataHorario.isEqual(rqc.dataCheckIn.get)) {
        DBIO.failed(new IllegalArgumentException("A data de check-out deve ser posterior à data de check-in."))
      } else {
        val novaReservaExistenteAction = Tables.reservasQuartos
          .filter(rq => rq.quartoId === checkOutDTO.quartoId && rq.reservaId =!= checkOutDTO.reservaId)
          .join(Tables.reservas)
          .on(_.reservaId === _.id)
          .filter({ case (rq, _) => rq.previsaoCheckIn > rqc.previsaoCheckIn })
          .exists
          .result

        novaReservaExistenteAction.flatMap { reservaExistente =>
          val updateDataCheckOut = Tables.reservasQuartos
            .filter(rq => rq.quartoId === checkOutDTO.quartoId && rq.reservaId === checkOutDTO.reservaId)
            .map(rq => rq.dataCheckOut)
            .update(Some(checkOutDTO.dataHorario))

          val finalAction =
            if (reservaExistente) {
              updateDataCheckOut.map(_ => NoContent())
            } else {
              val updateDisponivelAs = Tables.quartos
                .filter(_.id === checkOutDTO.quartoId)
                .map(_.disponivelAs)
                .update(Some(checkOutDTO.dataHorario.plusHours(4)))

              updateDataCheckOut.flatMap(_ => updateDisponivelAs.map(_ => NoContent()))
            }

          finalAction
        }
      }
    case None =>
      DBIO.failed(new NoSuchElementException(("Quarto ou reserva não localizada.")))
    }

    db.run(oq.transactionally)
  }

  def verificarTaxaOcupacao(data: LocalDateTime) = {
    log.info("Calculando taxa de ocupação...")

    val quantidadeTotalQuartosAction = Tables.quartos.length.result

    val quantidadeQuartosOcupadosAction = Tables.reservasQuartos
      .join(Tables.quartos)
      .on(_.quartoId === _.id)
      .filter({ case (rq, q) => rq.previsaoCheckIn <= data && q.disponivelAs >= data })
      .length
      .result

    db.run(quantidadeTotalQuartosAction.flatMap { totalQuartos =>
      quantidadeQuartosOcupadosAction.map { quartosOcupados =>
        val taxaOcupacao = if (totalQuartos > 0) {
          val ocupacao = quartosOcupados.toDouble / totalQuartos
          val percentual = ocupacao * 100
          BigDecimal(percentual).setScale(2, RoundingMode.HALF_EVEN).toDouble
        } else {
          0.0
        }
        taxaOcupacao
      }
    })
  }

}
