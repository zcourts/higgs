package info.crlog.higgs.protocols.websocket

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class JsonMessage extends Serializable {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  //uncomment while testing to ensure all expected fields are covered
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  @JsonProperty
  var topic: String = ""
  @JsonProperty
  var data = Map.empty[String, AnyRef]

  def +=(value: (String, AnyRef)) = data += value

  def has(key: String) = {
    data.get(key) match {
      case None => false
      case Some(v) => true
    }
  }

  def get[T](key: String) = {
    data.get(key) match {
      case None => None
      case Some(v) => Some(v.asInstanceOf[T])
    }
  }

  override def toString = mapper.writeValueAsString(this)
}

object JsonMessage {
  def apply(topic: String) = {
    val msg = new JsonMessage()
    msg.topic = topic
    msg
  }

  def apply(topic: String, data: Map[String, Any]) = {
    val msg = new JsonMessage()
    msg.topic = topic
    msg.data = data.asInstanceOf[Map[String, AnyRef]]
    msg
  }
}
