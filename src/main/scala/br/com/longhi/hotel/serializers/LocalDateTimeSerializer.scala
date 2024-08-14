package br.com.longhi.hotel.serializers

import org.json4s.{CustomSerializer, JNull, JString}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeSerializer extends CustomSerializer[LocalDateTime](format => (
  {
    case JString(date) => LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    case JNull => null
  },
  {
    case date: LocalDateTime => JString(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  }
))
