package info.crlog.higgs.messages

import io.netty.channel.Channel
import info.crlog.higgs.serializers.JsonSerializer
import org.codehaus.jackson.annotate.{JsonProperty, JsonIgnore}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class JsonMessage {
  @JsonIgnore
  var channel: Channel = null
  @JsonIgnore
  var serializer: JsonSerializer = null
  @JsonProperty
  var topic: String = ""
  @JsonProperty
  var data = Map.empty[String, AnyRef]

  def get[T](key: String) = {
    data.get(key) match {
      case None => None
      case Some(v) => Some(v.asInstanceOf[T])
    }
  }
}
