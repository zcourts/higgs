package info.crlog.higgs.protocols.boson.v1

import info.crlog.higgs.protocols.boson.{UnsupportedBosonTypeException, BosonType, Message}
import io.netty.buffer.{ByteBuf, HeapByteBuf}
import info.crlog.higgs.util.StringUtil
import java.{util, lang}
import collection.mutable
import lang.reflect.Field

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonWriter(obj: Message) {

  def get(): Array[Byte] = {
    //using Int.MaxValue as max buffer size since messages are limited to that size...
    val buffer = new HeapByteBuf(0, Int.MaxValue)
    val msg = if (obj.callback.isEmpty) {
      serializeResponse()
    } else {
      serializeRequest()
    }
    //first thing to write is the protocol version
    buffer.writeByte(obj.protocolVersion)
    //next the total size of the message (size excludes the first 5 bytes (protocol version and message size))
    buffer.writeInt(msg.length)
    //then write the message itself
    buffer.writeBytes(msg)
    buffer.resetReaderIndex()
    val ser = new Array[Byte](buffer.writerIndex())
    //read the BYTES WRITTEN into an array and return it
    buffer.getBytes(0, ser, 0, ser.length)
    ser
  }

  def serializeResponse(): Array[Byte] = {
    val buffer = new HeapByteBuf(0, Int.MaxValue)
    //write the method name
    buffer.writeByte(BosonType.RESPONSE_METHOD_NAME) //write type/flag - 1 byte
    writeString(buffer, obj.method)
    //write the parameters
    buffer.writeByte(BosonType.RESPONSE_PARAMETERS) //write type/flag - int = 4 bytes
    writeArray(obj.arguments, buffer) //write the size/length and payload
    buffer.resetReaderIndex()
    val ser = new Array[Byte](buffer.writerIndex())
    //read the BYTES WRITTEN into an array and return it
    buffer.getBytes(0, ser, 0, ser.length)
    ser
  }

  def serializeRequest(): Array[Byte] = {
    val buffer = new HeapByteBuf(0, Int.MaxValue)
    //write the method name
    buffer.writeByte(BosonType.REQUEST_METHOD_NAME) //write type/flag - 1 byte
    writeString(buffer, obj.method)
    //write the parameters
    buffer.writeByte(BosonType.REQUEST_PARAMETERS) //write type/flag - int = 4 bytes
    writeArray(obj.arguments, buffer) //write the size/length and payload
    //write the callback name
    buffer.writeByte(BosonType.REQUEST_CALLBACK) //write type/flag - 1 byte
    writeString(buffer, obj.callback)
    buffer.resetReaderIndex()
    val ser = new Array[Byte](buffer.writerIndex())
    //read the BYTES WRITTEN into an array and return it
    buffer.getBytes(0, ser, 0, ser.length)
    ser
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
    buffer.writeInt(i)
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
    buffer.writeByte(BosonType.STRING) //type
    val str = new StringUtil().getBytes(s)
    buffer.writeInt(str.length) //size
    buffer.writeBytes(str) //payload
  }

  def writeList(value: List[Any], buffer: ByteBuf) {
    buffer.writeByte(BosonType.LIST) //type
    buffer.writeInt(value.size) //size
    for (param <- value) {
      if (param == null) {
        writeNull(buffer)
      } else {
        validateAndWriteType(param, buffer) //payload
      }
    }
  }

  /**
   * Write an array of any supported boson type to the given buffer.
   * If the buffer contains any unsupported type, this will fail by throwing an UnsupportedBosonTypeException
   * @param buffer
   * @param value
   */
  def writeArray(value: Array[Any], buffer: ByteBuf) {
    buffer.writeByte(BosonType.ARRAY) //type
    buffer.writeInt(value.length) //size
    for (param <- value) {
      if (param == null) {
        writeNull(buffer)
      } else {
        validateAndWriteType(param, buffer) //payload
      }
    }
  }

  def writeMap(value: Map[Any, Any], buffer: ByteBuf) {
    buffer.writeByte(BosonType.MAP) //type
    buffer.writeInt(value.size) //size
    for ((key, value) <- value) {
      if (key == null) {
        writeNull(buffer)
      } else {
        validateAndWriteType(key, buffer) //key payload
      }
      if (value == null) {
        writeNull(buffer)
      } else {
        validateAndWriteType(value, buffer) //value payload
      }
    }
  }

  def writePolo(value: Any, buffer: ByteBuf): Boolean = {
    val obj = value.asInstanceOf[AnyRef]
    val klass = obj.getClass()
    val data = mutable.Map.empty[String, Any]
    //get public fields of the object and all its super classes
    val publicFields = klass.getFields
    //get ALL (public,private,protect,package) fields declared in the class - excludes inherited fields
    val classFields = klass.getDeclaredFields
    //create a set of fields removing duplicates
    val fields: Set[Field] = Set[Field]() ++ classFields ++ publicFields
    for (field <- fields) {
      //add if annotated with BosonProperty
      if (field.isAnnotationPresent(classOf[BosonProperty])) {
        field.setAccessible(true)
        val ann = field.getAnnotation(classOf[BosonProperty])
        val name = if (ann.value().isEmpty()) field.getName() else ann.value()
        data += name -> field.get(value)
      }
    }
    //if at least one field is annotated
    if (data.size > 0) {
      buffer.writeByte(BosonType.POLO) //type
      buffer.writeInt(data.size) //size
      for ((key, value) <- data) {
        var writeValue = true
        if (key == null) {
          //POLO keys cannot be null
          writeValue = false
        } else {
          //key must be a string!
          validateAndWriteType(key, buffer) //key payload
        }
        if (writeValue) {
          //keys can't be null, if they are ignore
          if (value == null) {
            writeNull(buffer)
          } else {
            validateAndWriteType(value, buffer) //value payload
          }
        }
      }
    }
    //if no fields found that can be serialized then the arguments array
    //length will be more than it should be.
    return data.size > 0
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
    } else if (obj.isArray ||
      classOf[Array[Any]].isAssignableFrom(obj)
    //TODO add support for treating a Scala Seq as an array since it is ordered
    //|| classOf[Seq[Any]].isAssignableFrom(obj)
    ) {
      writeArray(param.asInstanceOf[Array[Any]], buffer)
    } else if (classOf[List[Any]].isAssignableFrom(obj)
      || classOf[util.List[Any]].isAssignableFrom(obj)) {
      writeList(param.asInstanceOf[List[Any]], buffer)
    } else if (classOf[collection.Map[Any, Any]].isAssignableFrom(obj)
      || classOf[util.Map[Any, Any]].isAssignableFrom(obj)) {
      writeMap(param.asInstanceOf[Map[Any, Any]], buffer)
    } else {
      if (!writePolo(param, buffer)) {
        throw new UnsupportedBosonTypeException("%s is not a supported type, see BosonType for a list of supported types" format (obj.getName()), null)
      }
    }
  }
}
