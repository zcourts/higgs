package info.crlog.higgs.protocols.omsg

import info.crlog.higgs.Serializer
import info.crlog.higgs.protocols.omsg.SerializablePacker

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class OMsgSerializer[S] extends Serializer[S, Array[Byte]] {
  val packer = new SerializablePacker

  def serialize(obj: S) = packer.toBytes(obj)

  def deserialize(obj: Array[Byte]) = packer.get(obj).asInstanceOf[S]
}
