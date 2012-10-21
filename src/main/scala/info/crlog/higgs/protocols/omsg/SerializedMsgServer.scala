package info.crlog.higgs.protocols.omsg

import io.netty.channel.{Channel, ChannelHandlerContext}
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class SerializedMsgServer(host: String, port: Int)
  extends OMsgServer[OMsg[AnyRef]](host, port) {
  /**
   *
   * @param klass  classOf the message type OMsg encapsulates
   * @param fn
   * @tparam M
   */
  def listen[M <: Serializable](klass: Class[M], fn: (OMsg[M]) => Unit) {
    super.listen(classOf[OMsg[AnyRef]], (c: Channel, s: Serializable) => {
      val msg = s.asInstanceOf[OMsg[M]]
      if (msg.obj.isAssignableFrom(klass))
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
