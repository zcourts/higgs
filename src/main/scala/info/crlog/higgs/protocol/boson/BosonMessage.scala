package info.crlog.higgs.protocol.boson

import java.util.UUID
import info.crlog.higgs.protocol.Message
import reflect.BeanProperty

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class BosonMessage extends Message {
  def this(message: String) = {
    this ()
    contents = message.getBytes
  }

  /**
   * Create a message supplying its topic
   * @param t the topic of this message
   * @param c the contents of the message
   */
  def this(t: String, c: String) = {
    this ()
    topic = t
    contents = c.getBytes
  }

  def this(bytes: Array[Byte]) = {
    this ()
    contents = bytes
  }

  /**
   * Simple,naive constructor which simply calls toString on the provided object
   * i.e. your object must override toString and return the string form you wish to be sent
   */
    def this(obj: AnyRef) = {
      this ()
      contents = obj.toString.getBytes
    }

  @BeanProperty
  val id = UUID.randomUUID
}