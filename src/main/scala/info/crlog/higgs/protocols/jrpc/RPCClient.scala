package info.crlog.higgs.protocols.jrpc

import info.crlog.higgs.Client
import java.io.Serializable
import io.netty.channel.{Channel, ChannelHandlerContext}
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class RPCClient(serviceName: String,port: Int, host: String="localhost",  compress: Boolean = false)
  extends Client[String, RPC, Array[Byte]](serviceName, port, host, compress) {
  val serializer = new RPCSerializer()

  /**
   * any method which wants to receive a copy of ALL messages received
   * That method must accept an Array[Serializable] as its only parameter
   * @return The most generic form of topics supported
   */
  def allTopicsKey() = ""

  def decoder() = new RPCDecoder()

  def encoder() = new RPCEncoder()

  /**
   * Call a remote  method which you don't expect a response from.
   * @param remoteMethodName
   * @param params
   */
  def invoke(remoteMethodName: String, params: Serializable*) {
    invoke(remoteMethodName, (r: Option[AnyRef], t: Option[Throwable]) => {
      subscribers -= remoteMethodName
    }, params: _*)
  }

  /**
   * Do exactly the same as invoke except it blocks and doesn't return until a result is received
   * NOTE: The callback is invoked BEFORE the method returns as if it were a synchronous operation
   * @param remoteMethodName
   * @param callback
   * @param params
   */
  def invokeBlock[S <: Serializable, U](remoteMethodName: String,
                                        callback: (Option[S], Option[Throwable]) => U,
                                        params: Serializable*)(implicit mf: Manifest[S]) {
    val q = new LinkedBlockingQueue[String]()
    invoke(remoteMethodName, (o: Option[S], x: Option[Throwable]) => {
      callback(o, x)
      q.add("") //unblock
    }, params: _*)
    q.take() //block
  }

  /**
   * Invoke a method on the remote host
   * @param remoteMethodName the fully qualified name of the method e.g. com.domain.class.method
   * @param callback  a method that is invoked when/if a response is received
   * @param params a set of parameters that are to be passed to the remote method
   * @return
   */
  def invoke[S <: Serializable, U](remoteMethodName: String,
                                   callback: (Option[S], Option[Throwable]) => U,
                                   params: Serializable*)(implicit mf: Manifest[S]) {
    //remote_method_name:callback_id...remote_params
    val id = UUID.randomUUID().toString
    listen(id, (c: Channel, req: RPC) => {
      if (req.response != None && mf.erasure.isAssignableFrom(req.response.get.getClass)) {
        callback(Some(req.response.get.asInstanceOf[S]), req.error)
      } else {
        callback(None, Some(new IllegalResponseException(req.response)))
      }
    })
    super.send(new RPC(remoteMethodName, id, params.toArray))
    this
  }

  def send[T <: Serializable](msg: T)(implicit mf: Manifest[T]) = {
    //remote_method_name:callback_id...remote_params
    //in this case there is no call back not expecting a response and no remote method name
    super.send(new RPC("", "", Array(msg).asInstanceOf[Array[Serializable]]))
    this
  }


  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val data = serializer.deserialize(value)
    val topic = data.clientCallbackID
    notifySubscribers(context.channel(),
      topic,
      data
    )
  }


}
