package info.crlog.higgs.protocols.boson.json

import info.crlog.higgs.Client
import io.netty.channel.{Channel, ChannelHandlerContext}
import info.crlog.higgs.util.Algorithms

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonClient(host: String, port: Int, compress: Boolean = false) extends
Client[String, Message, Array[Byte]](host, port, compress) {
  val serializer = new BosonSerializer()

  def decoder() = new BosonDecoder()

  def encoder() = new BosonEncoder()

  def allTopicsKey() = ""

  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val data = serializer.deserialize(value)
    val topic = data.topic
    notifySubscribers(context.channel(),
      topic,
      data
    )
  }

  /**
   * Unlike the other invoke method this does not invoke the callback when an invalid
   * response is received...
   * @param method
   * @param arguments
   * @param callback
   * @param subscribe
   * @param mf
   * @tparam T
   * @tparam U
   * @return
   */
  def invoke[T, U](method: String, arguments: Seq[Any],
                   callback: (T) => U,
                   subscribe: Boolean)(implicit mf: Manifest[T]): String = {
    invoke(method, arguments, (msg: T, i: InvalidBosonResponse) => {
      if (i != null) {
        log.warn("Invalid boson message received ", i)
      } else {
        callback(msg)
      }
    }, subscribe)
  }

  /**
   * Invoke the given method on the remote host
   * @param method the name of the method on the remote host
   * @param arguments an order set of parameters to be passed to the remote method
   * @param callback a function to be invoked when a response is received from the remote host
   * @param subscribe if true then the given function listens for multiple responses until you
   *                  manually unsubscribe with the id returned. If false (default) then
   *                  as soon as a response is received the function is unsubscribed
   *                  and will not be invoked again
   * @param mf
   * @tparam T
   * @return  a string which is the ID given to the callback provided. Use for calls to
   *          unsubscribe(id)
   */
  def invoke[T, U](method: String, arguments: Seq[Any],
                   callback: (T, InvalidBosonResponse) => U = (msg: T, i: InvalidBosonResponse) => {},
                   subscribe: Boolean = false)(implicit mf: Manifest[T]): String = {
    val id = Algorithms.sha1(math.random)
    listen(id, (c: Channel, m: Message) => {
      if (!subscribe) {
        unsubscribe(id)
      }
      serializer.unmarshal[T,U](m,callback)
    })
    send(new Message(method, Some(arguments.asInstanceOf[Seq[AnyRef]]), Some(id)))
    id
  }
}
