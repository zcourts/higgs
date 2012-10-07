package info.crlog.higgs.omsg

import io.netty.channel.{Channel, ChannelHandlerContext}
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class SerializedMsgServer(host: String, port: Int)
  extends OMsgServer[OMsg[AnyRef]](host, port) {

  def listen[M <: Serializable](fn: (OMsg[M]) => Unit) {
    super.listen(classOf[OMsg[AnyRef]], (c: Channel, s: Serializable) => {
      fn(s.asInstanceOf[OMsg[M]])
    })
  }

  override def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val msg = serializer.deserialize(value)
    if (msg.isInstanceOf[OMsg[AnyRef]]) {
      msg.asInstanceOf[OMsg[AnyRef]].channel = context.channel()
      msg.asInstanceOf[OMsg[AnyRef]].serializer = serializer
      notifySubscribers(context.channel(),
        msg.getClass().asInstanceOf[Class[OMsg[AnyRef]]], msg)
    } else {
      super.message(context, value)
    }
  }

}
