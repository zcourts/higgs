package info.crlog.higgs.protocols.boson.v1

import info.crlog.higgs.protocols.boson.BosonType._
import org.junit.Test
import io.netty.buffer.{ByteBuf, Unpooled}
import org.junit.Assert._

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonReaderTest {
  val delta = 1e-15
  val fdelta: Float = 1e-15f

  def bufToArray(buf: ByteBuf) = {
    val arr = new Array[Byte](buf.writerIndex())
    buf.getBytes(0, arr, 0, buf.writerIndex())
    arr
  }

  def testBufToArrayLength(expected: Int, arr: Array[Byte]) {
    assertEquals("Array length should be %s" format (expected), expected, arr.length)
  }

  //writing tests with specs is nice but its slowing down the process. until i get used to it enough...
  @Test
  def testReadByte() {
    val value: Byte = Byte.MaxValue
    val buffer = Unpooled.buffer()
    buffer.writeByte(value)
    //verify the data we're putting in
    val data = bufToArray(buffer)
    testBufToArrayLength(1, data)
    val reader = new BosonReader(data)
    assertTrue("Read must be readable", reader.data.readable())
    //now verify the data we get out
    val byte = reader.readByte(true, BYTE)
    assertEquals("Written should get %s as a byte back" format (value), value, byte)
    assertEquals("Written %s byte, should get %s byte back" format(value, value), data(0), byte)
    assertFalse("Read %s byte, should no longer be readable" format (value), reader.data.readable())
  }

  @Test
  def testReadShort() {
    val value: Short = Short.MaxValue
    val buf = Unpooled.buffer()
    buf.writeShort(value)
    //verify what's written
    val data = bufToArray(buf)
    //short is 2 bytes
    testBufToArrayLength(2, data)
    val reader = new BosonReader(data)
    assertTrue("Reader must be readab;e", reader.data.readable())
    //verify what we get out
    val short = reader.readShort(true, SHORT)
    assertEquals("Written %s should get back same" format (value), value, short)
    assertFalse("Read a short, should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadInt() {
    val value: Int = Int.MaxValue
    val buf = Unpooled.buffer()
    buf.writeInt(value)
    //verify what's written
    val data = bufToArray(buf)
    //short is 4 bytes
    testBufToArrayLength(4, data)
    val reader = new BosonReader(data)
    assertTrue("Reader must be readable", reader.data.readable())
    //verify what we get out
    val int = reader.readInt(true, INT)
    assertEquals("Written %s should get back same" format (value), value, int)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadLong() {
    val value: Long = Long.MaxValue
    val buf = Unpooled.buffer()
    buf.writeLong(value)
    //verify what's written
    val data = bufToArray(buf)
    //short is 8 bytes
    testBufToArrayLength(8, data)
    val reader = new BosonReader(data)
    assertTrue("Reader must be readable", reader.data.readable())
    //verify what we get out
    val long = reader.readLong(true, LONG)
    assertEquals("Written %s should get back same" format (value), value, long)
    assertFalse("Read a long, should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadFloat() {
    val value: Float = Float.MaxValue
    val buf = Unpooled.buffer()
    buf.writeFloat(value)
    //verify what's written
    val data = bufToArray(buf)
    //short is 4 bytes
    testBufToArrayLength(4, data)
    val reader = new BosonReader(data)
    assertTrue("Reader must be readable", reader.data.readable())
    //verify what we get out
    val float = reader.readFloat(true, FLOAT)
    assertEquals("Written %s should get back same" format (value), value, float, delta)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadDouble() {
    val value: Double = Double.MaxValue
    val buf = Unpooled.buffer()
    buf.writeDouble(value)
    //verify what's written
    val data = bufToArray(buf)
    //short is 4 bytes
    testBufToArrayLength(8, data)
    val reader = new BosonReader(data)
    assertTrue("Reader must be readable", reader.data.readable())
    //verify what we get out
    val double = reader.readDouble(true, DOUBLE)
    assertEquals("Written %s should get back same" format (value), value, double, delta)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadChar() {
    val value: Char = Char.MaxValue
    val buf = Unpooled.buffer()
    buf.writeChar(value)
    //verify what's written
    val data = bufToArray(buf)
    //short is 4 bytes
    testBufToArrayLength(2, data)
    val reader = new BosonReader(data)
    assertTrue("Reader must be readable", reader.data.readable())
    //verify what we get out
    val char = reader.readChar(true, CHAR)
    assertEquals("Written %s should get back same" format (value), value, char)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadString() {
    val str = "Test string contents"
    val value: Array[Byte] = str.getBytes("utf-8")
    val buf = Unpooled.buffer()
    //string must be serialized as boson type -> string length -> string data
    buf.writeByte(STRING)
    buf.writeInt(value.length)
    buf.writeBytes(value)
    //verify what's written
    val data = bufToArray(buf)
    //string is 1 byte for type, 4 bytes for string length + length of string's byte array
    testBufToArrayLength(5 + value.length, data)
    val reader = new BosonReader(data)
    assertTrue("Reader must be readable", reader.data.readable())
    //verify what we get out
    //false, we've not read the first byte to verify the type is in deed BosonType.STRING
    val string = reader.readString(false, STRING)
    assertNotNull("resulting strung should not be null", string)
    assertEquals("Written \"%s\" should get back same" format (value), str, string)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadByteArray() {
    val value: Array[Byte] = Array[Byte](1, 2, 3)
    val buf = Unpooled.buffer()
    //array serialized as, boson type -> component type -> array length -> array data
    buf.writeByte(ARRAY)
    buf.writeByte(BYTE)
    //write array length/size
    buf.writeInt(value.length)
    //now write the values
    value foreach ((i: Byte) => {
      //int serialized as boson type -> value
      buf.writeByte(BYTE)
      buf.writeByte(i)
    })
    //no verify what we get out
    val reader = new BosonReader(bufToArray(buf))
    val array: Array[Byte] = reader.readArray(false, ARRAY).asInstanceOf[Array[Byte]]
    assertArrayEquals("resulting array should be the same", value, array)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadCharArray() {
    val value: Array[Char] = Array[Char]('a', 'b', 'c', 'e', 'f')
    val buf = Unpooled.buffer()
    //array serialized as, boson type -> component type -> array length -> array data
    buf.writeByte(ARRAY)
    buf.writeByte(CHAR)
    //write array length/size
    buf.writeInt(value.length)
    //now write the values
    value foreach ((i: Char) => {
      //int serialized as boson type -> value
      buf.writeByte(CHAR)
      buf.writeChar(i)
    })
    //no verify what we get out
    val reader = new BosonReader(bufToArray(buf))
    val array: Array[Char] = reader.readArray(false, ARRAY).asInstanceOf[Array[Char]]
    assertEquals(value.length, array.length)
    for (i <- 0 until value.length) {
      assertEquals(value(i), array(i))
    }
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadShortArray() {
    val value: Array[Short] = Array[Short](1, 2, 3)
    val buf = Unpooled.buffer()
    //array serialized as, boson type -> component type -> array length -> array data
    buf.writeByte(ARRAY)
    buf.writeByte(SHORT)
    //write array length/size
    buf.writeInt(value.length)
    //now write the values
    value foreach ((i: Short) => {
      //int serialized as boson type -> value
      buf.writeByte(SHORT)
      buf.writeShort(i)
    })
    //no verify what we get out
    val reader = new BosonReader(bufToArray(buf))
    val array: Array[Short] = reader.readArray(false, ARRAY).asInstanceOf[Array[Short]]
    assertArrayEquals("resulting array should be the same", value, array)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadIntArray() {
    val value = Array(1, 2, 3)
    val buf = Unpooled.buffer()
    //array serialized as, boson type -> component type -> array length -> array data
    buf.writeByte(ARRAY)
    buf.writeByte(INT)
    //write array length/size
    buf.writeInt(value.length)
    //now write the values
    value foreach ((i: Int) => {
      //int serialized as boson type -> value
      buf.writeByte(INT)
      buf.writeInt(i)
    })
    //no verify what we get out
    val reader = new BosonReader(bufToArray(buf))
    val array: Array[Int] = reader.readArray(false, ARRAY).asInstanceOf[Array[Int]]
    assertArrayEquals("resulting array should be the same", value, array)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadLongArray() {
    val value: Array[Long] = Array[Long](1, 2, 3)
    val buf = Unpooled.buffer()
    //array serialized as, boson type -> component type -> array length -> array data
    buf.writeByte(ARRAY)
    buf.writeByte(LONG)
    //write array length/size
    buf.writeInt(value.length)
    //now write the values
    value foreach ((i: Long) => {
      //int serialized as boson type -> value
      buf.writeByte(LONG)
      buf.writeLong(i)
    })
    //no verify what we get out
    val reader = new BosonReader(bufToArray(buf))
    val array: Array[Long] = reader.readArray(false, ARRAY).asInstanceOf[Array[Long]]
    assertArrayEquals("resulting array should be the same", value, array)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadFloatArray() {
    val value: Array[Float] = Array[Float](1.3f, 2.3f, 3.22f)
    val buf = Unpooled.buffer()
    //array serialized as, boson type -> component type -> array length -> array data
    buf.writeByte(ARRAY)
    buf.writeByte(FLOAT)
    //write array length/size
    buf.writeInt(value.length)
    //now write the values
    value foreach ((i: Float) => {
      //int serialized as boson type -> value
      buf.writeByte(FLOAT)
      buf.writeFloat(i)
    })
    //no verify what we get out
    val reader = new BosonReader(bufToArray(buf))
    val array: Array[Float] = reader.readArray(false, ARRAY).asInstanceOf[Array[Float]]
    assertArrayEquals(value, array, fdelta)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadDoubleArray() {
    val value: Array[Double] = Array[Double](1.3D, 2.3D, 3.22D)
    val buf = Unpooled.buffer()
    //array serialized as, boson type -> component type -> array length -> array data
    buf.writeByte(ARRAY)
    buf.writeByte(DOUBLE)
    //write array length/size
    buf.writeInt(value.length)
    //now write the values
    value foreach ((i: Double) => {
      //int serialized as boson type -> value
      buf.writeByte(DOUBLE)
      buf.writeDouble(i)
    })
    //no verify what we get out
    val reader = new BosonReader(bufToArray(buf))
    val array: Array[Double] = reader.readArray(false, ARRAY).asInstanceOf[Array[Double]]
    assertArrayEquals(value, array, delta)
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadBooleanArray() {
    val value: Array[Boolean] = Array[Boolean](true, false, false, true, true)
    val buf = Unpooled.buffer()
    //array serialized as, boson type -> component type -> array length -> array data
    buf.writeByte(ARRAY)
    buf.writeByte(BOOLEAN) //component type
    //write array length/size
    buf.writeInt(value.length)
    //now write the values
    value foreach ((i: Boolean) => {
      //int serialized as boson type -> value
      buf.writeByte(BOOLEAN)
      buf.writeBoolean(i)
    })
    //no verify what we get out
    val reader = new BosonReader(bufToArray(buf))
    val array: Array[Boolean] = reader.readArray(false, ARRAY).asInstanceOf[Array[Boolean]]
    assertEquals(value.length, array.length)
    for (i <- 0 until value.length) {
      assertEquals(value(i), array(i))
    }
    assertFalse("should no longer be readable", reader.data.readable())
  }

  @Test
  def testReadMap() {
    val value = Map("test" -> 1, 1 -> 2D, 2 -> 4F, 3 -> 5L)
    val buf = Unpooled.buffer()
    buf.writeByte(MAP)

  }

  @Test
  def testReadSimplePOLO() {}

  @Test
  def testReadNestedPOLO() {}
}
