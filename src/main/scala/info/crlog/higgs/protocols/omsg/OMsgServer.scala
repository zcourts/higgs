package info.crlog.higgs.protocols.omsg

import info.crlog.higgs.Server
import io.netty.channel.{Channel, ChannelHandlerContext}
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class OMsgServer[Topic <: Serializable](host: String, port: Int)
  extends Server[Class[Topic], Serializable, Array[Byte]](host, port, true) {
  val serializer = new OMsgSerializer[Serializable]()

  def decoder() = new OMsgDecoder()

  def encoder() = new OMsgEncoder()

  //doesn't override just provides a similar method
  def listen[M <: Serializable, T <: Class[M]](topic: T, fn: (Channel, M) => Unit) {
    //Class is invariant - you may have a Class[T] but it is not a Class[U] unless T=U, no matter any other relationship
    //but since we've constrained the arguments properly just cast to the expected types
    super.listen(topic.asInstanceOf[Class[Topic]], fn.asInstanceOf[(Channel, Serializable) => Unit])
  }

  /**
   * Listen for messages of a given type. When instances of the message is received
   * do further filtering using the "want" callback.
   * If the want callback returns false then the listener fn is not invoked for the given
   * message
   * @param topic
   * @param fn
   * @param want
   * @tparam M
   * @tparam T
   */
  def listen[M <: Serializable, T <: Class[M]](topic: T, fn: (Channel, M) => Unit,
                                               want: (M) => Boolean) {
    super.listen(topic.asInstanceOf[Class[Topic]],
      fn.asInstanceOf[(Channel, Serializable) => Unit],
      (c: Class[Topic], m: Serializable) => {
        if (!m.isInstanceOf[M]) {
          false
        } else {
          want(m.asInstanceOf[M])
        }
      }
    )
  }

  /**
   * Send a message to all* connected clients
   * @param obj the message to send. This will be passed to serializer.serialize
   */
  def broadcast(obj: Serializable) {
    if (!bound) {
      throw new IllegalStateException("Server needs to be bound before it can broadcast")
    }
    val serializedMessage = serializer.serialize(obj)
    channels foreach {
      case (id, channel) => channel.write(serializedMessage)
    }
  }


  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val msg = serializer.deserialize(value)
    notifySubscribers(context.channel(),
      msg.getClass().asInstanceOf[Class[Topic]], msg)
  }

  def allTopicsKey() = classOf[Serializable].asInstanceOf[Class[Topic]]
}
