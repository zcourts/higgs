package info.crlog.higgs.protocols.boson

import v1.BosonSerializer
import info.crlog.higgs.Client
import io.netty.channel.{Channel, ChannelHandlerContext}
import info.crlog.higgs.util.Algorithms

/**
 * @param serviceName A human friendly name of the remote service this client connects to. Because when
 *                    you have several clients failing to connect, its not easy to tell from host:port what's what.
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonClient(serviceName: String, port: Int, host: String = "localhost", compress: Boolean = false)
  extends Client[String, Message, Array[Byte]](serviceName: String, port, host, compress) {
  val serializer = new BosonSerializer()

  def decoder() = new BosonDecoder()

  def encoder() = new BosonEncoder()

  def allTopicsKey() = ""


  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val data = serializer.deserialize(value)
    val topic = data.method
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
  def invoke[T, U](method: String, arguments: Array[Any],
                   callback: (T) => U,
                   subscribe: Boolean)(implicit mf: Manifest[T]): String = {
    invoke(method, arguments, (msg: Option[T], i: Option[InvalidBosonResponse]) => {
      if (i != None) {
        log.warn("Invalid boson message received ", i.get)
      } else {
        callback(msg.get)
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
  def invoke[T, U](method: String, arguments: Array[Any],
                   callback: (Option[T], Option[InvalidBosonResponse]) => U =
                   (msg: Option[T], i: Option[InvalidBosonResponse]) => {},
                   subscribe: Boolean = false)
                  (implicit mf: Manifest[T]): String = {
    val id = Algorithms.sha1(math.random)
    listen(id, (c: Channel, m: Message) => {
      if (!subscribe) {
        unsubscribe(id)
      }
      if (classOf[Message] == mf.erasure) {
        //if the callback accepts Message just cast and pass it
        callback(Some(m.asInstanceOf[T]), None)
      } else {
        //if we have in fact received parameters to be passed to the callback
        if (m.arguments.length > 0) {
          val param = m.arguments(0)
          val klass = param.asInstanceOf[AnyRef].getClass()
          //if param is the same as or is a super class of the expected type, we can cast to it
          if (klass.isAssignableFrom(mf.erasure)) {
            callback(Some(param.asInstanceOf[T]), None)
          } else {
            val logmsg = "Remote method %s invoked, callback %s, " +
              "%s cannot be cast to %s" format(method, id, klass.getName(), mf.erasure.getName())
            log.warn(logmsg)
            callback(None, Some(new InvalidBosonResponse(logmsg, m, null)))
          }
        } else {
          log.info("Remote method %s invoked, acknowledgement" +
            " received with no data, unsubscribed %s" format(method, id))
        }
      }
    })
    send(new Message(method, arguments, id))
    id
  }
}
