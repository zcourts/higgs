package info.crlog.higgs.protocols.boson.v1

import org.specs2.mutable.Specification
import info.crlog.higgs.protocols.boson.Message
import info.crlog.higgs.protocols.boson.BosonType._
import io.netty.buffer.Unpooled

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonWriterTest extends Specification {
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

}
