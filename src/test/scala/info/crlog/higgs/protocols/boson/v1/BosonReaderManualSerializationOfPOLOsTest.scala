package info.crlog.higgs.protocols.boson.v1

import org.junit.Test
import io.netty.buffer.{ByteBuf, Unpooled}
import info.crlog.higgs.protocols.boson.BosonType._
import org.junit.Assert._

/**
 * Long messed up name but the point of this is simple
 * to manually serialize a POLO and use BosonReader to de-serialize.
 * If/When the protocol changes the stuff being manually serialized will need
 * changing but BosonReader needs to be tested without using BosonWriter
 * in case the writer is doing something wrong...manually of course we could
 * do something wrong but hey, that's why there's a specification, follow it!
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonReaderManualSerializationOfPOLOsTest {
  def bufToArray(buf: ByteBuf) = {
    val arr = new Array[Byte](buf.writerIndex())
    buf.getBytes(0, arr, 0, buf.writerIndex())
    arr
  }

  /**
   * This test manually writes an instance of the class "Polo"
   * then uses BosonReader to de-serialize it.
   */
  @Test
  def testWriteReadPOLO() {
    val polo = new Polo()
    polo.byte = 127
    val name = "byte"
    val className = "java.lang.Byte"
    //POLO serialized as,
    //Boson Type -> number of fields -> field name -> Fully qualified class name -> field value
    val buf = Unpooled.buffer()
    //need to write the protocol version and total size of the message
    buf.writeByte(1).writeInt(0).writeByte(REQUEST_PARAMETERS) //doesn't matter what they are
    buf.writeByte(POLO) //boson type
    buf.writeInt(1) //number of fields
    buf.writeBytes(name.getBytes("utf-8")) //field name
    buf.writeBytes(className.getBytes("utf-8")) //fully qualified class name
    //value is a byte, serialized as, Boson Type -> value
    buf.writeByte(BYTE)
    buf.writeByte(polo.byte)
    //now try to read it all back
    val reader = new BosonReader(bufToArray(buf))
//    reader.readPolo(false, BYTE)
//    assertTrue("Reader must be readable", reader.data.readable())
//    assertSame(polo.byte, reader.msg.arguments(0))
  }
}
