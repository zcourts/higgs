package info.crlog.higgs.protocols.boson.v1

import org.junit.Test

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonWriterTest  {
  //  sequential
  //  "BosonWriter" should {
  //    val reqmsg = new Message("test", Array(1, 2L, 3D, "abc"), "request")
  //    val reqwriter = new BosonWriter(reqmsg)
  //    val reqSerialized = reqwriter.get()
  //    val reqBuf = Unpooled.wrappedBuffer(reqSerialized)
  //    "Version is 1" in {
  //     val version= reqBuf.readByte() << 0
  //      version must beEqualTo(1)
  //    }
  //    "Message size should be more than 0 " in {
  //      reqBuf.readInt() must beGreaterThan(0)
  //    }
  //    "Serialize a request (method name, callback or parameters)" in {
  //      reqBuf.readInt() must beOneOf(
  //        REQUEST_METHOD_NAME,
  //        REQUEST_CALLBACK,
  //        REQUEST_PARAMETERS
  //      )
  //    }
  //  }
  //    new PoloExample(12345)
  @Test
  def testWriteByte() {
  }

  @Test
  def testWriteShort() {}

  @Test
  def testWriteInt() {}

  @Test
  def testWriteLong() {}

  @Test
  def testWriteFloat() {}

  @Test
  def testWriteDouble() {}

  @Test
  def testWriteChar() {}

  @Test
  def testWriteString() {}

  @Test
  def testWriteArray() {}

  @Test
  def testWriteMap() {}

  @Test
  def testWriteSimplePOLO() {}

  @Test
  def testWriteNestedPOLO() {}

}
