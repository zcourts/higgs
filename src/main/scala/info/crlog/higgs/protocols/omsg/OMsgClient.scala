package info.crlog.higgs.protocols.omsg

import info.crlog.higgs.Client
import io.netty.channel.{Channel, ChannelHandlerContext}
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
abstract class OMsgClient[Topic <: Serializable](serviceName: String, port: Int, host: String="localhost")
  extends Client[Class[Topic], Serializable, Array[Byte]](serviceName, port, host, true) {
  val serializer = new OMsgSerializer[Serializable]()

  def decoder() = new OMsgDecoder()

  def encoder() = new OMsgEncoder()

  def allTopicsKey() = classOf[Serializable].asInstanceOf[Class[Topic]]

  //doesn't override just provides a similar method
  def listen[M <: Serializable, T <: Class[M]](topic: T, fn: (Channel, M) => Unit) {
    //Class is invariant - you may have a Class[T] but it is not a Class[U] unless T=U, no matter any other relationship
    //but since we've constrained the arguments properly just cast to the expected types
    super.listen(topic.asInstanceOf[Class[Topic]],
      fn.asInstanceOf[(Channel, Serializable) => Unit])
  }

  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val msg = serializer.deserialize(value)
    //topic is guaranteed to be a sub class of Class[Serializable] so its somewhat safe to
    //assume casting works
    notifySubscribers(context.channel(),
      msg.getClass().asInstanceOf[Class[Topic]], msg)
  }
}
