package info.crlog.higgs.protocols.boson.v1

import info.crlog.higgs.protocols.boson.{InvalidDataException, UnsupportedBosonTypeException, InvalidRequestResponseTypeException, Message}
import io.netty.buffer.HeapByteBuf
import info.crlog.higgs.protocols.boson.BosonType._
import info.crlog.higgs.util.StringUtil
import collection.mutable
import collection.mutable.ListBuffer

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class BosonReader(obj: Array[Byte]) {
  val msg = new Message()
  //initialize a heap buffer setting the reader index to 0 and the writer index and capacity to array.length
  val data = new HeapByteBuf(obj, obj.length)
  //protocol version and message size is not a part of the message so read before loop
  //advance reader index by 1
  msg.protocolVersion = data.readByte()
  //move reader index forward by 4
  val msgSize = data.readInt()

  def get(): Message = {

    //so read until the reader index == obj.length
    while (data.readable()) {
      //read request/response types
      val Type: Int = data.readByte()
      Type match {
        case RESPONSE_METHOD_NAME => {
          msg.method = readString(false, 0)
        }
        case RESPONSE_PARAMETERS => {
          msg.arguments = readArray(false, 0)
        }
        case REQUEST_METHOD_NAME => {
          msg.method = readString(false, 0)
        }
        case REQUEST_CALLBACK => {
          msg.callback = readString(false, 0)
        }
        case REQUEST_PARAMETERS => {
          msg.arguments = readArray(false, 0)
        }
        case _ => throw new InvalidRequestResponseTypeException("The type %s does not match any of the supported" +
          " response or request types (method,callback,parameter)" format (Type), null)
      }
    }
    msg
  }

  /**
   * Check that the backing buffer is readable.
   * If it isn't throws an InvalidDataException
   * @throws InvalidDataException if buffer is not readable
   */
  def verifyReadable() {
    if (!data.readable()) {
      throw new InvalidDataException("BosonReader tried to read additional data from an unreadable buffer. " +
        "Possible data corruption.", null)
    }
  }

  /**
   * Read a UTF-8 string from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readString(verified: Boolean, verifiedType: Int): String = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (STRING == Type) {
      verifyReadable()
      //read size of type - how many bytes are in the string
      val size = data.readInt()
      //read type's payload and de-serialize
      new StringUtil().getString(data.readBytes(size).array())
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson STRING" format (Type), null)
    }
  }

  /**
   * Read a single byte from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readByte(verified: Boolean, verifiedType: Int): Byte = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (BYTE == Type) {
      verifyReadable()
      data.readByte()
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson BYTE" format (Type), null)
    }
  }

  /**
   * Read a short (16 bits) from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readShort(verified: Boolean, verifiedType: Int): Short = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (SHORT == Type) {
      verifyReadable()
      data.readShort()
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson SHORT" format (Type), null)
    }
  }

  /**
   * Read an int (4 bytes) from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readInt(verified: Boolean, verifiedType: Int): Int = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (INT == Type) {
      verifyReadable()
      data.readInt()
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson INT" format (Type), null)
    }
  }

  /**
   * Read a long (8 bytes) from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readLong(verified: Boolean, verifiedType: Int): Long = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (LONG == Type) {
      verifyReadable()
      data.readLong()
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson LONG" format (Type), null)
    }
  }

  /**
   * Read a float (32 bit floating point) from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readFloat(verified: Boolean, verifiedType: Int): Float = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (FLOAT == Type) {
      verifyReadable()
      data.readFloat()
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson FLOAT" format (Type), null)
    }
  }

  /**
   * Read a double (64 bit floating point) from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readDouble(verified: Boolean, verifiedType: Int): Double = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (DOUBLE == Type) {
      verifyReadable()
      data.readDouble()
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson DOUBLE" format (Type), null)
    }
  }

  /**
   * Read a a single byte from the buffer   if the byte is 1 then returns true, otherwise false
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readBoolean(verified: Boolean, verifiedType: Int): Boolean = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (BOOLEAN == Type) {
      verifyReadable()
      if (data.readByte() == 1) true else false
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson BOOLEAN" format (Type), null)
    }
  }

  /**
   * Read a char (16 bits) from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readChar(verified: Boolean, verifiedType: Int): Char = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (CHAR == Type) {
      verifyReadable()
      data.readChar()
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson CHAR" format (Type), null)
    }
  }

  /**
   * Read an array from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readArray(verified: Boolean, verifiedType: Int): Array[Any] = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (ARRAY == Type) {
      //read number of elements in the array
      val size = data.readInt()
      val arr = new Array[Any](size)
      for (i <- 0 until size) {
        verifyReadable()
        //get type of this element in the array
        val Type: Int = data.readByte()
        //at this stage only basic data types are allowed
        arr(i) = readType(Type)
      }
      arr
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson ARRAY" format (Type), null)
    }
  }

  /**
   * Read a List from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readList(verified: Boolean, verifiedType: Int): List[Any] = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (LIST == Type) {
      //read number of elements in the array
      val size = data.readInt()
      val arr = ListBuffer.empty[Any]
      for (i <- 0 until size) {
        verifyReadable()
        //get type of this element in the array
        val Type: Int = data.readByte()
        //at this stage only basic data types are allowed
        arr += readType(Type)
      }
      arr.toList
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson LIST" format (Type), null)
    }
  }

  /**
   * Read a map (list of key -> value pairs) from the buffer
   * @param verified  if true then the verifiedType param is used to match the Type, if false then
   *                  a single byte is read from the buffer to determine the type
   * @param verifiedType the data type to be de-serialized
   * @return
   */
  def readMap(verified: Boolean, verifiedType: Int): Map[Any, Any] = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (MAP == Type) {
      val size = data.readInt()
      val kv = mutable.Map.empty[Any, Any]
      for (i <- 0 until size) {
        //get type of with readByte() and use readType(Int) to extract/de-serialize
        verifyReadable()
        val key = readType(data.readByte())
        verifyReadable()
        val value = readType(data.readByte())
        kv += key -> value
      }
      kv.toMap //return immutable Map
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson MAP" format (Type), null)
    }
  }

  /**
   * Read the next type from the buffer.
   * The type param must match one of Boson's supported types otherwise an exception is thrown
   * @param Type
   * @return
   */
  def readType(Type: Int): Any = {
    Type match {
      case BYTE => readByte(true, Type)
      case SHORT => readShort(true, Type)
      case INT => readInt(true, Type)
      case LONG => readLong(true, Type)
      case FLOAT => readFloat(true, Type)
      case DOUBLE => readDouble(true, Type)
      case BOOLEAN => readBoolean(true, Type)
      case CHAR => readChar(true, Type)
      case NULL => null
      case STRING => readString(true, Type)
      case ARRAY => readArray(true, Type)
      case LIST => readList(true, Type)
      case MAP => readMap(true, Type)
      case _ => throw new UnsupportedBosonTypeException("Type %s is not a valid boson type" format (Type), null)
    }
  }
}