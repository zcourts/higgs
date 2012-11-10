package info.crlog.higgs.protocols.websocket

import info.crlog.higgs.Serializer
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.logging.{InternalLoggerFactory, InternalLogger}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import info.crlog.higgs.protocols.websocket.JsonMessage

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class JsonSerializer extends Serializer[JsonMessage, AnyRef] {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  //uncomment while testing to ensure all expected fields are covered
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def serialize(obj: JsonMessage) = new TextWebSocketFrame(mapper.writeValueAsString(obj))

  def deserialize(obj: AnyRef): JsonMessage = {
    try {
      mapper.readValue(obj.toString, classOf[JsonMessage])
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
