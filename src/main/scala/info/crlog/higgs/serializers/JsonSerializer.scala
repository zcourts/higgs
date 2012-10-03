package info.crlog.higgs.serializers

import info.crlog.higgs.Serializer
import com.codahale.jerkson.Json._
import info.crlog.higgs.messages.JsonMessage
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class JsonSerializer extends Serializer[JsonMessage, AnyRef] {
  def serialize(obj: JsonMessage) = new TextWebSocketFrame(generate(obj))

  def deserialize(obj: AnyRef): JsonMessage = parse[JsonMessage](obj.toString)
}
