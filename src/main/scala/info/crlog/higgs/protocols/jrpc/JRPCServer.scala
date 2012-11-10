package info.crlog.higgs.protocols.jrpc

import java.io.Serializable
import io.netty.channel.ChannelHandlerContext
import info.crlog.higgs.RPCServer

/**
 * Listens for String topics (method names)
 * and the message is an array of parameters for the topic/method
 * @author Courtney Robinson <courtney@crlog.info>
 */
class JRPCServer(host: String, port: Int, compress: Boolean = false)
  extends RPCServer[RPC](host, port, compress) {
  val serializer = new RPCSerializer()

  /**
   * Proxies to broadcast(Serializable)
   * @param obj the message to send. This will be passed to serializer.serialize
   */
  def broadcast(obj: RPC) {
    broadcast(obj.asInstanceOf[Serializable])
  }

  /**
   * Broadcast an object to all connected clients.
   * If the given object is an instance of RPC then it is sent as-is.
   * If the object is not an instance of RPC then it is wrapped in an RPC object
   * whose client callback is "listen".
   * This means, on the client side, the function subscribed to the topic
   * "listen" will receive the object. As such, that function must accept
   * AnyRef/Object as its parameter in order for it to be invoked.
   * @param obj
   */
  def broadcast(obj: Serializable) {
    if (!bound) {
      throw new IllegalStateException("Server needs to be bound before it can broadcast")
    }
    val rpc = if (obj.isInstanceOf[RPC]) obj.asInstanceOf[RPC]
    else {
      new RPC("broadcast", "listen", Seq(obj))
    }
    val serializedMessage = serializer.serialize(rpc)
    channels foreach {
      case (id, channel) => channel.write(serializedMessage)
    }
  }

  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val data = serializer.deserialize(value)
    val size = notifySubscribers(context.channel(),
      data.remoteMethodName, //first param is always a string which represents the method name
      data //all other arguments are args to be pass to the method
    )
    if (size == 0) {
      respond(context.channel(), new RPC(data, Seq(None,
        new RemoteMethodNotFoundException("Method %s not found" format (data.remoteMethodName)))))
    }
  }


  def getArguments(param: RPC): Seq[AnyRef] = param.arguments

  def clientCallback(param: RPC): String = param.clientCallbackID

  def newResponse(remoteMethodName: String, clientCallbackID: String,
                  response: Option[Serializable], error: Option[Throwable]): RPC = {
    new RPC(remoteMethodName, clientCallbackID, Seq(), response, error)
  }

  def decoder() = new RPCDecoder()

  def encoder() = new RPCEncoder()

  def allTopicsKey(): String = ""
}
