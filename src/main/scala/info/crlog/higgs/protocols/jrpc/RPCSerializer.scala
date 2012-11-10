package info.crlog.higgs.protocols.jrpc

import info.crlog.higgs.Serializer
import info.crlog.higgs.protocols.omsg.SerializablePacker
import info.crlog.higgs.protocols.jrpc.RPC

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class RPCSerializer extends Serializer[RPC, Array[Byte]] {
  val packer = new SerializablePacker

  def serialize(obj: RPC) = packer.toBytes(obj)

  def deserialize(obj: Array[Byte]) = packer.get(obj).asInstanceOf[RPC]
}
