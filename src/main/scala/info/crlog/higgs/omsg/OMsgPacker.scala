package info.crlog.higgs.omsg

import java.io._

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class OMsgPacker {
  def get(data: Array[Byte]) = {
    val in: ObjectInputStream = new ObjectInputStream(new ByteArrayInputStream(data))
    val obj: Any = in.readObject
    in.close
    obj
  }

  def toBytes(obj: Any) = {
    // Serialize to a byte array
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream
    val out = new ObjectOutputStream(bos)
    out.writeObject(obj)
    out.close
    // Get the bytes of the serialized object
    bos.toByteArray
  }
}

object OMsgPacker extends OMsgPacker {
}
