package info.crlog.higgs.protocols.boson

import info.crlog.higgs.RPCServer
import io.netty.channel.ChannelHandlerContext
import java.io.Serializable
import v1.BosonSerializer

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
      notifySubscribers(context.channel(),
        data.method,
        data
      )
    } catch {
      case e => {
        log.warn("Unable to deserialize message ", e)
        respond(context.channel(),
          new Message("deserialize_error", Array(Map("msg" -> "Incomplete or Invalid Boson message received")))
        )
      }
    }
  }

  override def verifyArgumentType(parameters: Array[AnyRef], methodArguments: Array[Class[_]]): Boolean = {
    if (parameters.length != methodArguments.length) {
      return false //don't invoke
    }
    for (i <- 0 until parameters.length) {
      val methodParam = methodArguments(i)
      val param = parameters(i)
      //if its an array its a bit tricky so types need to be validated carefully
      if (param != null && param.getClass.isArray()) {
        if (!methodParam.isArray()) {
          return false //param received is an array but method isn't expecting one
        }
      }
    }
    true
  }
}
