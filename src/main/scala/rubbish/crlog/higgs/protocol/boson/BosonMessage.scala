package rubbish.crlog.higgs.protocol.boson

import scala.Predef._
import rubbish.crlog.higgs.protocol.Buffer
import rubbish.crlog.higgs.{ByteConvertible, Message, BosonString}
import io.netty.buffer.{ChannelBuffers, ChannelBuffer}

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */
class BosonMessage extends Message {
  protected val properties = scala.collection.mutable.Map.empty[Buffer, Buffer]

  def this(str: String) = {
    this()
    addProperty(new BosonString("content"), str)
  }

  def this(topic: String, content: String) = {
    this()
    addProperty(new BosonString("content"), content)
    addProperty(new BosonString("topic"), topic)
  }

  def this(content: Array[Byte]) = {
    this()
    deSerialize(ChannelBuffers.wrappedBuffer(content))
  }

  def this(content: ChannelBuffer) = {
    this()
    deSerialize(content)
  }

  def this(obj: Any) = {
    this()
    //TODO handle arbitrary objects
  }

  def getProperty(key: Buffer): Option[Buffer] = {
    properties.get(key)
  }

  /**
   * Get the property with the specified key
   * @param key the property's key
   * @return the value of the property or None if nothing is found for this key
   */
  def getProperty(key: BosonString): Option[BosonString] = {
    properties.get(Buffer.wrap(key.asBytes)) match {
      case None => None
      case Some(buffer) => {
        if (buffer.hasArray) {
          Some(new BosonString(buffer.array()))
        } else {
          return None
        }
      }
    }
  }

  def addProperty(key: Buffer, value: Buffer) {
    properties.put(key, value)
  }

  /**
   * Add a single property to this message.
   * @param key this property's key
   * @param value the property's value
   */
  def addProperty(key: BosonString, value: BosonString) {
    properties.put(Buffer.wrap(key.asBytes), Buffer.wrap(value.asBytes))
  }

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
  def addProperty(key: Array[Byte], value: Array[Byte]) {
    if (key.length == 0 || value.length == 0) {
      //TODO handle cases where a property being added has no key or value
    } else {
      properties.put(Buffer.wrap(key), Buffer.wrap(value))
    }
  }

  def addProperty(key: String, value: String) {
    addProperty(key.getBytes, value.getBytes)
  }

  /**
   * The idea here is that the message implementation should know how to convert
   * each property to a byte array and should only do the conversion when this method is invoked
   * and instead store properties internally in a more usable form. i.e. if it was a map of
   * strings  store the strings as is and when this method is invoked call getBytes on each string.
   *
   * @return A map of all the properties in this message as a byte array
   */
  def getProperties(): Array[Byte] = {
    serialize().array()
  }

  def deSerialize(buf: ChannelBuffer) {
    version = buf.readShort()
    //read all properties
    while (buf.readable()) {
      //read short, i.e. 16 bits/2 bytes
      val keySize = buf.readShort()
      //read keySize
      val key = buf.readBytes(keySize)
      //read value size,  32 bits , i.e. signed int
      val valueSize = buf.readInt()
      //read Value's bytes
      val value = buf.readBytes(valueSize)
      //add the property
      addProperty(key.array(), value.array())
    }
  }

  def serialize() = {
    val buf: ChannelBuffer = ChannelBuffers.dynamicBuffer
    //put the protocol version
    buf.writeShort(version)
    properties.map({
      case (key, value) => {
        //add the property key's size
        buf.writeShort(key.array().length)
        //add the property key
        buf.writeBytes(key.buffer())
        //add the property value's size
        buf.writeInt(value.array().length)
        //add the property value
        buf.writeBytes(value.buffer())
      }
    })
    buf
  }

  /**
   * Get the property with the specified key
   * @param key the property's key
   * @return the value of the property or None if nothing is found for this key
   */
  def getProperty(key: ByteConvertible): Option[ByteConvertible] = {
    getProperty(new Buffer(key.asString))
  }

  /**
   * Add a single property to this message.
   * @param key this property's key
   * @param value the property's value
   */
  def addProperty(key: ByteConvertible, value: ByteConvertible) {
    addProperty(Buffer.wrap(key.asBytes), Buffer.wrap(value.asBytes))
  }

  override def toString() = {
    val buf = new StringBuilder()
    //put the protocol version
    buf.append("v:").append(version)
    properties.map({
      case (key, value) => {
        buf.append(key.asString).append(":").append(value).append(";")
      }
    })
    buf.toString()
  }
}

