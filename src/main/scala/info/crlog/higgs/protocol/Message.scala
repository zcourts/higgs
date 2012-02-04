package info.crlog.higgs.protocol

import reflect.BeanProperty
import java.nio.charset.Charset
import com.codahale.jerkson.Json._
import com.codahale.jerkson.JsonSnakeCase

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 01/02/12
 */
@JsonSnakeCase
abstract class Message {
  @BeanProperty
  var topic: String = ""

  @BeanProperty
  var contents: Array[Byte] = null
  /**
   * An alias for <code>asString</code> which returns the contents of this message using the default charset for decoding  the message
   */
  override def toString(): String = {
    asString()
  }

  /**
   *  Gets the contents of this message as a string using the default system charset  to decode the message contents
   */
  def asString(): String = {
    asString(Charset.defaultCharset())
  }

  /**
   * Gets the contents of this message as a string using the specified charset to decode the message's contents
   */
  def asString(charset: Charset): String = {
    new String(contents, charset)
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
    generate(this).getBytes
  }
}