package info.crlog.higgs.messages

import org.codehaus.jackson.annotate.JsonProperty
import com.codahale.jerkson.Json._
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class JsonMessage extends Serializable {
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

  override def toString = generate(this)
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
