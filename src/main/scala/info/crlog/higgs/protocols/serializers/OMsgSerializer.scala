package info.crlog.higgs.protocols.serializers

import info.crlog.higgs.Serializer
import info.crlog.higgs.protocols.omsg.OMsgPacker

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class OMsgSerializer[S] extends Serializer[S, Array[Byte]] {
  def serialize(obj: S) = OMsgPacker.toBytes(obj)

  def deserialize(obj: Array[Byte]) = OMsgPacker.get(obj).asInstanceOf[S]
}
