package info.crlog.higgs.protocols.boson.v1

import info.crlog.higgs.protocols.boson._
import io.netty.buffer.HeapByteBuf
import info.crlog.higgs.protocols.boson.BosonType._
import info.crlog.higgs.util.StringUtil
import collection.mutable
import collection.mutable.ListBuffer
import info.crlog.higgs.protocols.boson.Message
import info.crlog.higgs.protocols.boson.UnsupportedBosonTypeException
import info.crlog.higgs.protocols.boson.InvalidRequestResponseTypeException
import org.slf4j.LoggerFactory
import java.lang.{IllegalArgumentException, reflect}
import reflect.Field

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class BosonReader(obj: Array[Byte]) {
  val loader = new BosonClassLoader(Thread.currentThread().getContextClassLoader())
  val log = LoggerFactory.getLogger(getClass())
  val msg = new Message()
  //initialize a heap buffer setting the reader index to 0 and the writer index and capacity to array.length
  val data = new HeapByteBuf(obj, obj.length)

  def get(): Message = {
    //protocol version and message size is not a part of the message so read before loop
    //advance reader index by 1
    msg.protocolVersion = data.readByte()
    //move reader index forward by 4
    val msgSize = data.readInt()

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
      val buf = data.readBytes(size)
      val arr = new Array[Byte](buf.writerIndex())
      buf.getBytes(0, arr)
      new StringUtil().getString(arr)
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
   * @param klass If not null and klass.isArray()==true then initialize an array of the
   *              component type of klass. If any of these premise are false an array[Any]
   *              is used
   * @return
   */
  def readArray(verified: Boolean, verifiedType: Int, klass: Class[_] = null): Array[_] = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (ARRAY == Type) {
      //  read component type
      val componentType = data.readByte()
      //read number of elements in the array
      val size = data.readInt()
      if (klass != null) {
        //if we have a class read array of its type
        readObjectArrayFromClass(klass, size)
      } else {
        val arr = componentType match {
          case BOOLEAN => readBoolArray(size)
          case BYTE => readByteArray(size)
          case CHAR => readCharArray(size)
          case DOUBLE => readDoubleArray(size)
          case FLOAT => readFloatArray(size)
          case INT => readIntArray(size)
          case LONG => readLongArray(size)
          case SHORT => readShortArray(size)
          case _ => {
            readObjectArray(size)
          }
        }
        arr
      }
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson ARRAY" format (Type), null)
    }
  }

  def readBoolArray(size: Int) = {
    val arr = new Array[Boolean](size)
    for (i <- 0 until arr.length) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      value match {
        case b: Boolean => arr(i) = b
        case _ => throw new UnsupportedBosonTypeException(
          "Cannot assign %s of type %s to array of type %s" format(value, value.asInstanceOf[AnyRef].getClass,
            arr.getClass().getComponentType()), null)
      }
    }
    arr
  }

  def readByteArray(size: Int) = {
    val arr = new Array[Byte](size)
    for (i <- 0 until arr.length) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      value match {
        case b: Byte => arr(i) = b
        case _ => throw new UnsupportedBosonTypeException(
          "Cannot assign %s of type %s to array of type %s" format(value, value.asInstanceOf[AnyRef].getClass,
            arr.getClass().getComponentType()), null)
      }
    }
    arr
  }

  def readCharArray(size: Int) = {
    val arr = new Array[Char](size)
    for (i <- 0 until arr.length) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      value match {
        case b: Char => arr(i) = b
        case _ => throw new UnsupportedBosonTypeException(
          "Cannot assign %s of type %s to array of type %s" format(value, value.asInstanceOf[AnyRef].getClass,
            arr.getClass().getComponentType()), null)
      }
    }
    arr
  }

  def readShortArray(size: Int) = {
    val arr = new Array[Short](size)
    for (i <- 0 until arr.length) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      value match {
        case b: Short => arr(i) = b
        case _ => throw new UnsupportedBosonTypeException(
          "Cannot assign %s of type %s to array of type %s" format(value, value.asInstanceOf[AnyRef].getClass,
            arr.getClass().getComponentType()), null)
      }
    }
    arr
  }

  def readIntArray(size: Int) = {
    val arr = new Array[Int](size)
    for (i <- 0 until arr.length) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      value match {
        case b: Int => arr(i) = b
        case _ => throw new UnsupportedBosonTypeException(
          "Cannot assign %s of type %s to array of type %s" format(value, value.asInstanceOf[AnyRef].getClass,
            arr.getClass().getComponentType()), null)
      }
    }
    arr
  }

  def readLongArray(size: Int) = {
    val arr = new Array[Long](size)
    for (i <- 0 until arr.length) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      value match {
        case b: Long => arr(i) = b
        case _ => throw new UnsupportedBosonTypeException(
          "Cannot assign %s of type %s to array of type %s" format(value, value.asInstanceOf[AnyRef].getClass,
            arr.getClass().getComponentType()), null)
      }
    }
    arr
  }

  def readFloatArray(size: Int) = {
    val arr = new Array[Float](size)
    for (i <- 0 until arr.length) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      value match {
        case b: Float => arr(i) = b
        case _ => throw new UnsupportedBosonTypeException(
          "Cannot assign %s of type %s to array of type %s" format(value, value.asInstanceOf[AnyRef].getClass,
            arr.getClass().getComponentType()), null)
      }
    }
    arr
  }

  def readDoubleArray(size: Int) = {
    val arr = new Array[Double](size)
    for (i <- 0 until arr.length) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      value match {
        case b: Double => arr(i) = b
        case _ => throw new UnsupportedBosonTypeException(
          "Cannot assign %s of type %s to array of type %s" format(value, value.asInstanceOf[AnyRef].getClass,
            arr.getClass().getComponentType()), null)
      }
    }
    arr
  }

  def readObjectArray(size: Int): Array[Any] = {
    val arr = new Array[Any](size)
    for (i <- 0 until size) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      arr(i) = value
    }
    arr
  }

  def readObjectArrayFromClass[T](klass: Class[T], size: Int): Array[T] = {
    val arr = reflect.Array.newInstance(klass, size)
    for (i <- 0 until size) {
      verifyReadable()
      //get type of this element in the array
      val Type: Int = data.readByte()
      val value = readType(Type)
      reflect.Array.set(arr, i, value)
    }
    arr.asInstanceOf[Array[T]]
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
        verifyReadable()
        val keyType = data.readByte()
        val key = readType(keyType)
        val valueType = data.readByte()
        val value = readType(valueType)
        kv += key -> value
      }
      kv.toMap //return immutable Map
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson MAP" format (Type), null)
    }
  }

  def readPolo(verified: Boolean, verifiedType: Int) = {
    val Type: Int = if (verified) verifiedType else data.readByte()
    if (POLO == Type) {
      verifyReadable()
      //get class name
      val poloClassName = readString(false, -1)
      if (poloClassName == null || poloClassName.isEmpty) {
        throw new InvalidDataException("Cannot de-serialise a POLO without it's fully qualified class name being provided", null)
      }
      //get number of fields serialized
      val size = data.readInt()
      //try to load the class if available
      try {
        val klass = loader.loadClass(poloClassName)
        val instance = klass.newInstance()
        //superclass's fields
        val sf = klass.getSuperclass()
        val publicFields = if (sf == null) Array.empty[Field] else sf.getDeclaredFields()
        //get ALL (public,private,protect,package) fields declared in the class - excludes inherited fields
        val classFields = klass.getDeclaredFields
        //create a map of fields names -> Field
        val fieldset = (for (f <- (classFields ++ publicFields)) yield {
          f.getName() -> f
        }).toMap

        for (i <- 0 until size) {
          verifyReadable()
          //polo keys are required to be strings
          val key = readString(false, 0)
          verifyReadable()
          val valueType = data.readByte()
          val value: Any = readType(valueType)
          fieldset get key match {
            case Some(field) => {
              field.setAccessible(true)
              //if field's type is an array  create an array of it's type
              val fieldType = field.getType()
              //TODO add support for Scala's Seq - || fieldType.isAssignableFrom()
              if (fieldType.isArray()) {
                if (value == null) {
                  log.warn("Field \":%s\" of class \"%s\" is an array but null".format(key, klass.getName()))
                } else if (value.asInstanceOf[AnyRef].getClass().isArray()) {
                  val length = reflect.Array.getLength(value)
                  //create an array of the expected type
                  val arr = reflect.Array.newInstance(fieldType.getComponentType(), length)
                  for (i <- 0 until length) {
                    try {
                      //get current array value
                      val arrayValue = reflect.Array.get(value, i)
                      reflect.Array.set(arr, i, arrayValue) //set the value at the current index, i
                    } catch {
                      case e: IllegalArgumentException => {
                        log.warn("Field \":%s\" of class \"%s\" is an array but failed to set value at index \"%s\" - type \"%s\"".format(
                          key, klass.getName(), i, if (value == null) "null" else value.asInstanceOf[AnyRef].getClass().getName()))
                      }
                    }
                  }
                  field.set(instance, arr)
                } else {
                  log.warn("Field \":%s\" of class \"%s\" is an array but value received is \"%s\" of type \"%s\"".format(
                    key, klass.getName(), value, if (value == null) "null" else value.asInstanceOf[AnyRef].getClass().getName()
                  ))
                }
              } else {
                if (value != null) {
                  val vclass = value.asInstanceOf[AnyRef].getClass()
                  if (field.getType().isAssignableFrom(vclass)) {
                    field.set(instance, value)
                  } else {
                    log.warn(("Field \"%s\" of class \"%s\" is of type %s " +
                      "but value received is \"%s\" of type \"%s\"").format(
                      key, klass.getName(), vclass.getName(), value,
                      if (value == null) "null" else value.asInstanceOf[AnyRef].getClass().getName()
                    ))
                  }
                }
              }
            }
            case None => {
              log.warn(("Field %s received with value %s but the " +
                "field does not exist in class %s").format(key, value, poloClassName))
            }
          }
        }
        instance
      } catch {
        case cnfe: ClassNotFoundException => {
          log.error("Class %s received but not found on classpath, value left as POLOContainer" format (poloClassName), cnfe)
          null
        }
      }
    } else {
      throw new UnsupportedBosonTypeException("Type %s is not a Boson POLO" format (Type), null)
    }
  }

  /**
   * Read the next type from the buffer.
   * The type param must match one of Boson's supported types otherwise an exception is thrown
   * @param Type  the 1 byte integer representing a Boson type
   * @param klass if present and Type is an array this should be the component type of the array, it's passed to readArray
   * @return
   */
  def readType(Type: Int, klass: Class[_] = null): Any = {
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
      case ARRAY => readArray(true, Type, klass)
      case LIST => readList(true, Type)
      case MAP => readMap(true, Type)
      case POLO => readPolo(true, Type)
      case _ => throw new UnsupportedBosonTypeException("Type %s is not a valid boson type" format (Type), null)
    }
  }
}