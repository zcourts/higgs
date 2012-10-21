package info.crlog.higgs.protocols.serializers

import info.crlog.higgs.Serializer
import com.codahale.jerkson.Json._
import info.crlog.higgs.messages.JsonMessage
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.logging.{InternalLoggerFactory, InternalLogger}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class JsonSerializer extends Serializer[JsonMessage, AnyRef] {
  def serialize(obj: JsonMessage) = new TextWebSocketFrame(generate(obj))

  def deserialize(obj: AnyRef): JsonMessage = {
    try {
      parse[JsonMessage](obj.toString)
    } catch {
      case e => {
        val log: InternalLogger = InternalLoggerFactory.getInstance(getClass)
        log.warn("Unable to deserialize message %s".format(obj), e)
        val m = new JsonMessage()
        m.topic = "json_deserialize_error"
        m +=("msg", obj)
        m
      }
    }
  }
}
