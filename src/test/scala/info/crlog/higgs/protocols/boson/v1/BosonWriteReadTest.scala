package info.crlog.higgs.protocols.boson.v1

import org.specs2.mutable.Specification
import info.crlog.higgs.protocols.boson.Message
import java.util

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonWriteReadTest extends Specification {
  val reqArgs = Array(1, 2L, 3D, "abc", Map("a" -> 1), List(1, 2, 3), Array("a", "b", "c", 1, 2, 3))
  val reqmsg = new Message("test", reqArgs, "request")
  val reqwriter = new BosonWriter(reqmsg)
  val reqSerialized = reqwriter.get()
  //val reqBuf = Unpooled.copiedBuffer(reqSerialized)
  val reqreader = new BosonReader(reqSerialized)
  val reqDeserialized = reqreader.get()

  "Boson v 1 protocol" should {
    "Only version 1 protocol is allowed " in versionIsOne
    "The de-serialized version must match the version before serialization" in version
    "Serialized and De-serialized callback must be the same" in samecallback
    "Serialized and De-serialized parameters must be the same" in sameparams
  }

  def sameparams = util.Arrays.deepEquals(reqArgs.asInstanceOf[Array[AnyRef]],
    reqDeserialized.arguments.asInstanceOf[Array[AnyRef]]) must beEqualTo(true)

  def samecallback = reqDeserialized.callback must beEqualTo(reqmsg.callback)

  def version = reqDeserialized.protocolVersion must beEqualTo(reqmsg.protocolVersion)

  def versionIsOne = reqDeserialized.protocolVersion must beEqualTo(1) and {
    reqmsg.protocolVersion must beEqualTo(1)
  }

  //TODO repeat all the above tests for response messages
}
