package br.com.longhi.hotel.exceptions

class ParameterNotFoundException(val msg: String, val cause: Exception) extends Exception(msg, cause) {

  def this(msg: String) = this(msg, null)

}
