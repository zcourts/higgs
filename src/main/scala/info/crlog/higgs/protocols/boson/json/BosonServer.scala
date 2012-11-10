package info.crlog.higgs.protocols.boson.json

import info.crlog.higgs.RPCServer
import io.netty.channel.{Channel, ChannelHandlerContext}
import java.io.Serializable
import info.crlog.higgs.util.StringUtil
import org.codehaus.jackson.map.JsonMappingException

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonServer(host: String, port: Int, compress: Boolean = false)
  extends RPCServer[Message](host, port, compress) {

  val serializer = new BosonSerializer()

  def decoder() = new BosonDecoder()

  def encoder() = new BosonEncoder()

  def allTopicsKey(): String = ""

  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    try {
      val data = serializer.deserialize(value)
      val size = notifySubscribers(context.channel(),
        data.topic,
        data
      )
      if (size == 0) {
        respond(context.channel(),
          new Message(data.callback.getOrElse("error"),None,None,Map("msg" -> "Method %s not found".format(data.topic), "error" -> "not_found"))
        )
      }
    } catch {
      case e: JsonMappingException => {
        val data = StringUtil.getString(value)
        log.warn("Unable to deserialize message %s".format(data), e)
        respond(context.channel(),
          new Message("json_deserialize_error", None,None,
            Map("msg" -> "Incomplete or Invalid JSON received %s ".format(data))
          )
        )
      }
    }
  }

  def broadcast(obj: Message) {}

  def getArguments(param: Message): Seq[AnyRef] = {
    param.arguments match {
      case None => Seq()
      case Some(arr) => arr
    }
  }

  def clientCallback(param: Message): String = {
    param.callback match {
      case None => ""
      case Some(c) => c
    }
  }

  def newResponse(remoteMethodName: String, clientCallbackID: String,
                  response: Option[Serializable], error: Option[Throwable]) = {
//    val data: Map[String, Any] = Map(
//      "remote_method" -> remoteMethodName
//    ) ++
//      (response match {
//        case None => Map()
//        case Some(r) => Map("response" -> r)
//      }) ++
//      (error match {
//        case None => Map()
//        case Some(e) => Map("error" -> e)
//      })

    new Message(clientCallbackID,None,None, response.getOrElse(null))
  }

  def listen[T, U](topic: String, callback: (T) => U)(implicit mf: Manifest[T]) {
    listen(topic, (msg: T, i: InvalidBosonResponse) => {
      if (i != null) {
        log.warn("Invalid boson message received ", i)
      } else {
        callback(msg)
      }
    })

  }

  /**
   * Listen for a topic being broadcasted
   * @param topic
   * @param callback
   * @param mf
   * @tparam T
   * @tparam U
   * @return
   */
  def listen[T, U](topic: String, callback: (T, InvalidBosonResponse) => U)
                  (implicit mf: Manifest[T]) {
    listen(topic, (c: Channel, m: Message) => {
      serializer.unmarshal(m, (msg: T, i: InvalidBosonResponse) => {
        callback(msg, i)
      })
    })
  }
}
