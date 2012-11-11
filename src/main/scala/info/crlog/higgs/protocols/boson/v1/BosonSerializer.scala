package info.crlog.higgs.protocols.boson.v1

import info.crlog.higgs.Serializer
import info.crlog.higgs.protocols.boson.Message

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonSerializer extends Serializer[Message, Array[Byte]] {

  def serialize(obj: Message): Array[Byte] = {
    new BosonWriter(obj).get()
  }

  def deserialize(obj: Array[Byte]): Message = {
    new BosonReader(obj).get()
  }
}
