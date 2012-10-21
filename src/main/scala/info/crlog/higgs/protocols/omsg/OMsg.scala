package info.crlog.higgs.protocols.omsg

import java.util.UUID
import io.netty.channel.Channel
import java.io.Serializable
import info.crlog.higgs.protocols.serializers.OMsgSerializer

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class OMsg[T]
(msg: T, id: UUID = UUID.randomUUID()) extends Serializable {
  var channel: Channel = null
  var serializer: OMsgSerializer[Serializable] = null
  val obj = msg.asInstanceOf[AnyRef].getClass

  def respond(m: Any) {
    channel.write(serializer.serialize(new OMsg(m, id)))
  }
}
