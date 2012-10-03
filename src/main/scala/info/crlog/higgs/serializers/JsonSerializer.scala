package info.crlog.higgs.serializers

import websocket.Message

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class JsonSerializer extends Serializer[Message,String]{
  def serialize(obj: Message): Array[Byte] = null

  def deserialize(obj: String): Message = null
}
