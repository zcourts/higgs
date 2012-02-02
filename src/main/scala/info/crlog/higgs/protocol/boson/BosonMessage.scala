package info.crlog.higgs.protocol.boson

import java.util.UUID
import info.crlog.higgs.protocol.Message
import com.codahale.jerkson.JsonSnakeCase
import reflect.BeanProperty

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

@JsonSnakeCase
case class BosonMessage(private var message: String) extends Message {
  contents = message getBytes

  def this(bytes: Array[Byte]) = {
    this ("")
    contents = bytes
  }

  /**
   * Simple,naive constructor which simply calls toString on the provided object
   * i.e. your object must override toString and return the string form you wish to be sent
   */
  def this(obj: AnyRef) = {
    this (obj.toString)
  }

  @BeanProperty
  val id = UUID.randomUUID
}