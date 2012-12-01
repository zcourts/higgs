package info.crlog.higgs.protocols.boson.v1

import info.crlog.higgs.protocols.boson.BosonType
import io.netty.buffer.{Unpooled, ByteBuf}
import java.{util, lang}
import collection.mutable.ListBuffer
import lang.reflect.Field
import info.crlog.higgs.protocols.boson.BosonType._
import info.crlog.higgs.protocols.boson.Message
import info.crlog.higgs.protocols.boson.UnsupportedBosonTypeException

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonWriter(obj: Message) {
  def get(): Array[Byte] = {
    //using Int.MaxValue as max buffer size since messages are limited to that size...
    val buffer = Unpooled.buffer()
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
    val buffer = Unpooled.buffer()
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
    val buffer = Unpooled.buffer()
    //write the method name
    buffer.writeByte(BosonType.REQUEST_METHOD_NAME) //write type/flag - 1 byte
    writeString(buffer, obj.method)
    //write the callback name
    buffer.writeByte(BosonType.REQUEST_CALLBACK) //write type/flag - 1 byte
    writeString(buffer, obj.callback)
    //write the parameters
    buffer.writeByte(BosonType.REQUEST_PARAMETERS) //write type/flag - int = 4 bytes
    writeArray(obj.arguments, buffer) //write the size/length and payload
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
    val str = s.getBytes("utf-8")
    buffer.writeInt(str.length) //size
    buffer.writeBytes(str) //payload
  }

  def writeList(value: List[Any], buffer: ByteBuf) {
    buffer.writeByte(BosonType.LIST) //type
    buffer.writeInt(value.size) //size
    val it = value.iterator
    while (it.hasNext) {
      val param = it.next()
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
  def writeArray(value: Array[_], buffer: ByteBuf) {
    buffer.writeByte(ARRAY) //type
    //we write the component type of the array or null if its not an array
    val component = getArrayComponent(value.getClass())
    buffer.writeByte(component)
    buffer.writeInt(value.length) //size
    for (param <- value) {
      validateAndWriteType(param, buffer) //payload
    }
  }

  def writeMap(value: collection.Map[Any, Any], buffer: ByteBuf) {
    buffer.writeByte(BosonType.MAP) //type
    buffer.writeInt(value.size) //size
    val it = value.keysIterator
    while (it.hasNext) {
      val key = it.next()
      val v = value(key)
      validateAndWriteType(key, buffer) //key payload
      validateAndWriteType(v, buffer) //value payload
    }
  }

  def writePolo(value: Any, buffer: ByteBuf): Boolean = {
    if (value == null) {
      validateAndWriteType(value, buffer)
      return false
    }
    val obj = value.asInstanceOf[AnyRef]
    val klass = obj.getClass()
    val data = new util.HashMap[String, Any]()
    //get super class's public fields
    val sc = klass.getSuperclass()
    val publicFields = if (sc == null) Array.empty[Field] else sc.getDeclaredFields()
    //get ALL (public,private,protect,package) fields declared in the class - excludes inherited fields
    val classFields = klass.getDeclaredFields
    var ignoreInheritedFields = false
    if (klass.isAnnotationPresent(classOf[BosonProperty])) {
      ignoreInheritedFields = klass.getAnnotation(classOf[BosonProperty]).ignoreInheritedFields()
    }
    //process inherited fields first - don't ignore inherited fields by default
    for (field <- publicFields) {
      val annotated = field.isAnnotationPresent(classOf[BosonProperty])
      //add if annotated with BosonProperty
      if (annotated
        || !ignoreInheritedFields) {
        field.setAccessible(true)
        var name = field.getName()
        if (annotated) {
          val ann = field.getAnnotation(classOf[BosonProperty])
          if (ann != null && annotated && ann.value().isEmpty()) {
            name = ann.value()
          }
        }
        data.put(name, field.get(value))
      }
    }
    //process fields declared in the class itself
    for (field <- classFields) {
      field.setAccessible(true)
      var add = true
      var name = field.getName()
      //add if annotated with BosonProperty and not marked as ignored
      if (field.isAnnotationPresent(classOf[BosonProperty])) {
        val ann = field.getAnnotation(classOf[BosonProperty])
        if (ann.ignore()) {
          add = false
        }
        if (!ann.value().isEmpty()) {
          name = ann.value() //use name given in annotation if not empty
        }
      }
      if (add) {
        //overwriting even if previously set when public fields were processed
        data.put(name, field.get(value))
      }
    }
    //if at least one field is allowed to be serialized
    if (data.size() > 0) {
      buffer.writeByte(BosonType.POLO) //type
      writeString(buffer, klass.getName()) //class name
      buffer.writeInt(data.size) //size
      val it = data.keySet().iterator()
      while (it.hasNext()) {
        val key = it.next()
        val value = data.get(key)
        writeString(buffer, key) //key payload must be a string
        validateAndWriteType(value, buffer) //value payload
      }
    }
    //if no fields found that can be serialized then the arguments array
    //length will be more than it should be.
    return data.size > 0
  }

  /**
   * The JVM would return the java keywords int, long etc for all primitive types
   * on an array using the rules outlined below.
   * This is of no use when serializing/de-serializing so this method converts
   * java primitive names to their boson data type equivalent.
   * The rest of this java doc is from Java's Class class
   * which details how it treats array of primitives.
   *
   * <p> If this class object represents a primitive type or void, then the
   * name returned is a {@code String} equal to the Java language
   * keyword corresponding to the primitive type or void.
   *
   * <p> If this class object represents a class of arrays, then the internal
   * form of the name consists of the name of the element type preceded by
   * one or more '{@code [}' characters representing the depth of the array
   * nesting.  The encoding of element type names is as follows:
   *
   * <blockquote><table summary="Element types and encodings">
   * <tr><th> Element Type <th> &nbsp;&nbsp;&nbsp; <th> Encoding
   * <tr><td> boolean      <td> &nbsp;&nbsp;&nbsp; <td align=center> Z
   * <tr><td> byte         <td> &nbsp;&nbsp;&nbsp; <td align=center> B
   * <tr><td> char         <td> &nbsp;&nbsp;&nbsp; <td align=center> C
   * <tr><td> class or interface
   * <td> &nbsp;&nbsp;&nbsp; <td align=center> L<i>classname</i>;
   * <tr><td> double       <td> &nbsp;&nbsp;&nbsp; <td align=center> D
   * <tr><td> float        <td> &nbsp;&nbsp;&nbsp; <td align=center> F
   * <tr><td> int          <td> &nbsp;&nbsp;&nbsp; <td align=center> I
   * <tr><td> long         <td> &nbsp;&nbsp;&nbsp; <td align=center> J
   * <tr><td> short        <td> &nbsp;&nbsp;&nbsp; <td align=center> S
   * </table></blockquote>
   *
   * <p> The class or interface name <i>classname</i> is the binary name of
   * the class specified above.
   *
   * <p> Examples:
   * <blockquote><pre>
   * String.class.getName()
   * returns "java.lang.String"
   * byte.class.getName()
   * returns "byte"
   * (new Object[3]).getClass().getName()
   * returns "[Ljava.lang.Object;"
   * (new int[3][4][5][6][7][8][9]).getClass().getName()
   * returns {@code "[[[[[[[ I "}
   * </pre></blockquote>
   *
   * @return the fully qualified class name of a java primitive or null if the class
   *         is not an array
   */
  def getArrayComponent(klass: Class[_ <: AnyRef]): Int = {
    val name = if (klass.isArray()) klass.getComponentType().getName() else null
    name match {
      case "boolean" | "java.lang.Boolean" => BOOLEAN
      case "byte" | "java.lang.Byte" => BYTE
      case "char" | "java.lang.Character" => CHAR
      case "double" | "java.lang.Double" => DOUBLE
      case "float" | "java.lang.Float" => FLOAT
      case "int" | "java.lang.Integer" => INT
      case "long" | "java.lang.Long" => LONG
      case "short" | "java.lang.Short" => SHORT
      case _ => {
        POLO
      }
    }
  }

  def validateAndWriteType(param: Any, buffer: ByteBuf) {
    if (param == null) {
      writeNull(buffer)
    } else {
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
      }
      //put check for list BEFORE check for Seq
      else if (classOf[List[Any]].isAssignableFrom(obj)
        || classOf[ListBuffer[Any]].isAssignableFrom(obj)
        || classOf[util.List[Any]].isAssignableFrom(obj)) {
        if (classOf[ListBuffer[Any]].isAssignableFrom(obj)) {
          writeList(param.asInstanceOf[ListBuffer[Any]].toList, buffer)
        } else if (classOf[List[Any]].isAssignableFrom(obj)) {
          writeList(param.asInstanceOf[List[Any]], buffer)
        } else {
          import collection.JavaConversions._
          writeList(param.asInstanceOf[util.List[Any]].toList, buffer)
        }
      } else if (obj.isArray ||
        classOf[Array[Any]].isAssignableFrom(obj)
        //it must be a Seq and not a List or ListBuffer (both extends LinearSeq)
        || (param.isInstanceOf[Seq[Any]] && !param.isInstanceOf[List[Any]] && !param.isInstanceOf[ListBuffer[Any]]) //classOf[Seq[Any]].isAssignableFrom(obj)
      ) {
        if (param.isInstanceOf[Seq[Any]]) {
          writeArray(param.asInstanceOf[Seq[Any]].toArray, buffer)
        } else {
          writeArray(param.asInstanceOf[Array[_]], buffer)
        }
      } else if (classOf[collection.Map[Any, Any]].isAssignableFrom(obj)
        || classOf[util.Map[Any, Any]].isAssignableFrom(obj)) {
        if (classOf[util.Map[Any, Any]].isAssignableFrom(obj)) {
          import collection.JavaConversions._
          writeMap(param.asInstanceOf[util.Map[Any, Any]].toMap, buffer)
        } else {
          writeMap(param.asInstanceOf[collection.Map[Any, Any]], buffer)
        }
      } else {
        if (!writePolo(param, buffer)) {
          throw new UnsupportedBosonTypeException("%s is not a supported type, see BosonType for a list of supported types" format (obj.getName()), null)
        }
      }
    }
  }
}
