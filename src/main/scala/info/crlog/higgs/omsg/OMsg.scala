package info.crlog.higgs.omsg

import java.util.UUID
import io.netty.channel.Channel
import info.crlog.higgs.serializers.OMsgSerializer
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class OMsg[T]
(msg: T, id: UUID = UUID.randomUUID()) extends Serializable {
  var channel: Channel = null
  var serializer: OMsgSerializer[Serializable] = null

  def respond(m: Any) {
    channel.write(serializer.serialize(new OMsg(m, id)))
  }
}
