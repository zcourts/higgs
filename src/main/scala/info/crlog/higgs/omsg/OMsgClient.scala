package info.crlog.higgs.omsg

import info.crlog.higgs.Client
import info.crlog.higgs.serializers.OMsgSerializer
import io.netty.channel.{Channel, ChannelHandlerContext}
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class OMsgClient(host: String, port: Int)
  extends Client[Class[Serializable], Serializable, Array[Byte]](host, port, true) {
  val serializer = new OMsgSerializer[Serializable]()

  def decoder() = new OMsgDecoder()

  def encoder() = new OMsgEncoder()

  def allTopicsKey() = classOf[Serializable]

  //doesn't override just provides a similar method
  def listen[M <: Serializable, T <: Class[M]](topic: T, fn: (Channel, M) => Unit) {
    //Class is invariant - you may have a Class[T] but it is not a Class[U] unless T=U, no matter any other relationship
    //but since we've constrained the params properly just cast to the expected types
    super.listen(topic.asInstanceOf[Class[Serializable]], fn.asInstanceOf[(Channel, Serializable) => Unit])
  }

  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val msg = serializer.deserialize(value)
    notifySubscribers(context.channel(),
      msg.getClass().asInstanceOf[Class[Serializable]], msg)
  }
}
