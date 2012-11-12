package info.crlog.higgs.protocols.boson

import info.crlog.higgs.RPCServer
import io.netty.channel.ChannelHandlerContext
import java.io.Serializable
import v1.{POLOContainer, BosonSerializer}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonServer(port: Int, host: String = "localhost", compress: Boolean = false)
  extends RPCServer[Message](host, port, compress) {
  val serializer = new BosonSerializer()

  def getArguments(param: Message) = param.arguments.asInstanceOf[Array[AnyRef]]

  def clientCallback(param: Message): String = param.callback

  def decoder() = new BosonDecoder()

  def encoder() = new BosonEncoder()

  def allTopicsKey(): String = ""

  def newResponse(remoteMethodName: String, clientCallbackID: String,
                  response: Option[Serializable], error: Option[Throwable]): Message = {
    val argResponse: Array[Any] = response match {
      case None => Array.empty[Any]
      case Some(arg) => Array(arg)
    }
    val argError: Array[Any] = error match {
      case None => Array.empty[Any]
      case Some(err) => Array(Map("error" -> err.getMessage()))
    }
    val args = argResponse ++ argError
    new Message(clientCallbackID, args)
  }

  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    try {
      val data = serializer.deserialize(value)
      val size = notifySubscribers(context.channel(),
        data.method,
        data
      )
      if (size == 0) {
        respond(context.channel(),
          //first param in array is always a response, since we have no response set to null
          new Message(data.callback, Array(null, Map(
            "msg" -> "Method %s not found".format(data.method),
            "error" -> "not_found"
          )))
        )
      }
    } catch {
      case e => {
        log.warn("Unable to deserialize message ", e)
        respond(context.channel(),
          new Message("deserialize_error", Array(Map("msg" -> "Incomplete or Invalid Boson message received")))
        )
      }
    }
  }

  def broadcast(obj: Message) {}

 override def verifyArgumentType(parameters: Array[AnyRef], methodArguments: Array[Class[_]]): Boolean = {
    if (parameters.length != methodArguments.length) {
      return false //don't invoke
    }
    var ok = true
    for (i <- 0 until parameters.length) {
      val param: AnyRef = {
        val p = parameters(i)
        //if its a polo
        if (p.isInstanceOf[POLOContainer]) {
          val polo = p.asInstanceOf[POLOContainer]
          val tmp = polo.as(methodArguments(i), true)
          val ar = tmp.asInstanceOf[AnyRef]
          //update the args with the polo
          parameters(i) = ar
          ar
        } else {
          p
        }
      }
      val methodParam = methodArguments(i)
      //if the param accepted by the server method is NOT the same as or a super class of
      if (!methodParam.isAssignableFrom(param.getClass)) {
        ok = false
      }
    }
    ok
  }
}
