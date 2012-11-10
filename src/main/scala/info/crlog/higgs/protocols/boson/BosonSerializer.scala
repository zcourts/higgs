package info.crlog.higgs.protocols.boson

import info.crlog.higgs.Serializer

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonSerializer extends Serializer[Message, Array[Byte]] {
  def serialize(obj: Message): Array[Byte] = {
    null
  }

  def deserialize(obj: Array[Byte]): Message = {
    null
  }
}
