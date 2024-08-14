package br.com.longhi.hotel.serializers

import org.json4s.{CustomSerializer, JNull, JString}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LocalDateSerializer extends CustomSerializer[LocalDate](format => (
  {
    case JString(date) => LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
    case JNull => null
  },
  {
    case date: LocalDate => JString(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
  }
))
