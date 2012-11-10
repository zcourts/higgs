package info.crlog.higgs.protocols.boson

import info.crlog.higgs.Serializer
import io.netty.buffer.{ByteBuf, HeapByteBuf}
import java.{util, lang}
import info.crlog.higgs.util.StringUtil

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonSerializer extends Serializer[Message, Array[Byte]] {

  def serialize(obj: Message): Array[Byte] = {
    //using Int.MaxValue as max buffer size since messages are limited to that size...
    val buffer = new HeapByteBuf(0, Int.MaxValue)
    val msg = if (obj.callback.isEmpty) {
      serializeResponse(obj)
    } else {
      serializeRequest(obj)
    }
    //first thing to write is the protocol version
    buffer.writeByte(obj.protocolVersion)
    //next the total size of the message (size excludes the first 5 bytes (protocol version and message size))
    buffer.writeInt(msg.length)
    //then write the message itself
    buffer.writeBytes(msg)
    buffer.resetReaderIndex()
    buffer.readSlice(buffer.writerIndex()).array()
  }

  def serializeResponse(message: Message): Array[Byte] = {
    val buffer = new HeapByteBuf(0, Int.MaxValue)
    //write the method name
    buffer.writeByte(BosonType.RESPONSE_METHOD_NAME) //write type/flag - 1 byte
    val method = StringUtil.getBytes(message.method)
    buffer.writeInt(method.length) //the length of the method name
    buffer.writeBytes(method) //write type's payload
    //write the parameters
    writeParameters(buffer, message.arguments) //write the parameter payload
    buffer.resetReaderIndex()
    buffer.readSlice(buffer.writerIndex()).array()
  }

  def serializeRequest(message: Message): Array[Byte] = {
    null
  }

  def deserialize(obj: Array[Byte]): Message = {
    null
  }

  /**
   * Write an array of any supported boson types to the given buffer.
   * If the buffer contains any unsupported type, this will fail by throwing an UnsupportedBosonTypeException
   * @param buffer
   * @param value
   */
  def writeParameters(buffer: ByteBuf, value: Array[Any]) {
    buffer.writeByte(BosonType.RESPONSE_PARAMETERS) //write type/flag - int = 4 bytes
    writeArray(value, buffer)
  }

  def writeByte(buffer: ByteBuf, b: Byte) {
    buffer.writeByte(BosonType.BYTE)
    buffer.writeByte(b)
  }

  def writeNull(buffer: ByteBuf) {
    buffer.writeByte(BosonType.NULL)
  }

  def writeShort(buffer: ByteBuf, s: Short) {
    buffer.writeByte(BosonType.SHORT)
    buffer.writeShort(s)
  }

  def writeInt(buffer: ByteBuf, i: Int) {
    buffer.writeByte(BosonType.INT)
    buffer.writeByte(i)
  }

  def writeLong(buffer: ByteBuf, l: Long) {
    buffer.writeByte(BosonType.LONG)
    buffer.writeLong(l)
  }

  def writeFloat(buffer: ByteBuf, f: Float) {
    buffer.writeByte(BosonType.FLOAT)
    buffer.writeFloat(f)
  }

  def writeDouble(buffer: ByteBuf, d: Double) {
    buffer.writeByte(BosonType.DOUBLE)
    buffer.writeDouble(d)
  }

  def writeBoolean(buffer: ByteBuf, b: Boolean) {
    buffer.writeByte(BosonType.BOOLEAN)
    buffer.writeByte(if (b) 1 else 0)
  }

  def writeChar(buffer: ByteBuf, c: Char) {
    buffer.writeByte(BosonType.CHAR)
    buffer.writeChar(c)
  }

  def writeString(buffer: ByteBuf, s: String) {
    buffer.writeByte(BosonType.STRING)
    val str = StringUtil.getBytes(s)
    buffer.writeInt(str.length)
    buffer.writeBytes(str)
  }

  def writeList(value: List[Any], buffer: ByteBuf) {
    buffer.writeByte(BosonType.LIST)
    buffer.writeInt(value.size)
    for (param <- value) {
      if (param == null) {
        writeNull(buffer)
      } else {
        validateAndWriteType(param, buffer)
      }
    }
  }

  def writeArray(value: Array[Any], buffer: ByteBuf) {
    buffer.writeByte(BosonType.ARRAY)
    buffer.writeInt(value.length)
    for (param <- value) {
      if (param == null) {
        writeNull(buffer)
      } else {
        validateAndWriteType(param, buffer)
      }
    }
  }

  def writeMap(value: Map[Any, Any], buffer: ByteBuf) {
    buffer.writeByte(BosonType.MAP)
    buffer.writeInt(value.size)
    for ((key, value) <- value) {
      if (key == null) {
        writeNull(buffer)
      } else {
        validateAndWriteType(key, buffer)
      }
      if (value == null) {
        writeNull(buffer)
      } else {
        validateAndWriteType(value, buffer)
      }
    }
  }

  def validateAndWriteType(param: Any, buffer: ByteBuf) {
    val obj = param.asInstanceOf[AnyRef].getClass
    if (obj == classOf[Byte] || obj == classOf[lang.Byte]) {
      writeByte(buffer, param.asInstanceOf[Byte])
    } else if (obj == classOf[Short] || obj == classOf[lang.Short]) {
      writeShort(buffer, param.asInstanceOf[Short])
    } else if (obj == classOf[Int] || obj == classOf[lang.Integer]) {
      writeInt(buffer, param.asInstanceOf[Int])
    } else if (obj == classOf[Long] || obj == classOf[lang.Long]) {
      writeLong(buffer, param.asInstanceOf[Long])
    } else if (obj == classOf[Float] || obj == classOf[lang.Float]) {
      writeFloat(buffer, param.asInstanceOf[Float])
    } else if (obj == classOf[Double] || obj == classOf[lang.Double]) {
      writeDouble(buffer, param.asInstanceOf[Double])
    } else if (obj == classOf[Boolean] || obj == classOf[lang.Boolean]) {
      writeBoolean(buffer, param.asInstanceOf[Boolean])
    } else if (obj == classOf[Char] || obj == classOf[lang.Character]) {
      writeChar(buffer, param.asInstanceOf[Char])
    } else if (obj == classOf[String] || obj == classOf[lang.String]) {
      writeString(buffer, param.asInstanceOf[String])
    } else if (obj.isArray || obj.isAssignableFrom(classOf[Array[Any]]) || obj.isAssignableFrom(classOf[Seq[Any]])) {
      writeArray(param.asInstanceOf[Array[Any]], buffer)
    } else if (obj.isAssignableFrom(classOf[List[Any]]) || obj.isAssignableFrom(classOf[util.List[Any]])) {
      writeList(param.asInstanceOf[List[Any]], buffer)
    } else if (obj.isAssignableFrom(classOf[Map[Any, Any]]) || obj.isAssignableFrom(classOf[util.Map[Any, Any]])) {
      writeMap(param.asInstanceOf[Map[Any, Any]], buffer)
    } else {
      throw new UnsupportedBosonTypeException("%s is not a supported type, see BosonType for a list of supported types" format (obj.getName()), null)
    }
  }
}
