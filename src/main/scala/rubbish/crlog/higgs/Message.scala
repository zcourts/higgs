package rubbish.crlog.higgs

import protocol.Version
import util.StringUtilities
import io.netty.buffer.ChannelBuffer

/**
 * NOTE: Messages need to have a channel associated with them to be able to respond
 * Responding to a message is "context free" the only difference between using
 * Messages.send and rubbish.crlog.higgs.Message.respond is rubbish.crlog.higgs.Message.respond directly sends a message back to
 * where ever the first message came from. Otherwise/In general sending a message goes out to all subscribed
 * recipients
 * Courtney Robinson <courtney@crlog.rubbish>
 */
trait Message {
  protected val properties: scala.collection.mutable.Map[_ <: ByteConvertible, _ <: ByteConvertible]
  protected val util = new StringUtilities
  var version: Short = Version.V1.version

  /**
   * Get the property with the specified key
   * @param key the property's key
   * @return the value of the property or None if nothing is found for this key
   */
  def getProperty(key: ByteConvertible): Option[ByteConvertible]

  /**
   * Add a single property to this message.
   * @param key this property's key
   * @param value the property's value
   */

  def addProperty(key: ByteConvertible, value: ByteConvertible)

  /**
   * When de-serializing properties are ready one at a time.
   * This method is invoked once per property and is expected for the implementation to
   * update its properties list with each property passed in via this method.
   * In both cases the implementation is expected to know how to deserialize the byte array of the key
   * and value.
   *
   * @param key   a byte array which is the key/name of this property
   * @param value the value of this property.
   */
  def addProperty(key: Array[Byte], value: Array[Byte])

  /**
   * The idea here is that the message implementation should know how to convert
   * each property to a byte array and should only do the conversion when this method is invoked
   * and instead store properties internally in a more usable form. i.e. if it was a map of
   * strings  store the strings as is and when this method is invoked call getBytes on each string.
   *
   * @return A map of all the properties in this message as a byte array
   */
  def getProperties(): Array[Byte]

  /**
   * Every message must be able to serialize itself
   * @return a byte buffer ready to be sent
   */
  def serialize(): ChannelBuffer

  /**
   * Every message must be able to de-serialize a byte buffer and populate it's contents
   */
  def deSerialize(buf: ChannelBuffer)
}

