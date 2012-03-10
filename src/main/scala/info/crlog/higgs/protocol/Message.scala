package info.crlog.higgs.protocol

import reflect.BeanProperty
import com.codahale.jerkson.JsonSnakeCase
import info.crlog.higgs.api.HiggsConstants
import info.crlog.higgs.util.StringUtil

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 01/02/12
 */
@JsonSnakeCase
abstract class Message {
  @BeanProperty
  var topic: String = HiggsConstants.TOPIC_ALL

  var contents = new StringBuffer()

  /**
   * An alias for <code>asString</code> which returns the contents of this message using the default charset for decoding  the message
   */
  override def toString(): String = {
    asString()
  }

  /**
   *  Gets the contents of this message as a string using the default Higgs charset  to decode the message contents
   */
  def asString(): String = {
    contents.toString
  }

  /**
   * Alias for serialize.
   * Serialize this message to a series of bytes that can be de-serialized on the other end
   */
  def asBytes(): Array[Byte] = {
    serialize
  }

  /**
   * Serialize this message to a series of bytes that can be de-serialized on the other end
   */
  def serialize(): Array[Byte] = {
    StringUtil.getBytes(contents.toString)
  }

  def setContents(m: String) = {
    contents.append(m)
  }

  implicit def msgToString(m: Message): String = {
    m.toString
  }

  implicit def msgToBytes(m: Message): Array[Byte] = {
    m.asBytes
  }
}