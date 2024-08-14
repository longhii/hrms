package br.com.longhi.hotel

import java.time.{LocalDate, LocalDateTime}

object DTO {

  case class Reserva(hospedeId: Long,
                     dataCheckIn: LocalDateTime,
                     dataCheckOut: LocalDate,
                     quartos: List[Long]) {
    override def toString: String =
      List(hospedeId, dataCheckIn, dataCheckOut, quartos.mkString(",")).mkString(";")
  }

  case class FluxoEntradaSaida(reservaId: Long, quartoId: Long, dataHorario: LocalDateTime)

}
