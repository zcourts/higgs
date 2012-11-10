package info.crlog.higgs.protocols.boson

import info.crlog.higgs.{Serializer, RPCServer}
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.{ByteToMessageDecoder, MessageToByteEncoder}
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonServer(port:Int,host:String="localhost",compress:Boolean=false)
extends RPCServer[Message](host,port,compress){
  val serializer: Serializer[Message, Array[Byte]] = _

  def getArguments(param: Message): Seq[AnyRef] = param.arguments

  def clientCallback(param: Message): String = null

  def newResponse(remoteMethodName: String, clientCallbackID: String, response: Option[Serializable], error: Option[Throwable]): Message = null

  def decoder(): ByteToMessageDecoder[Array[Byte]] = null

  def encoder(): MessageToByteEncoder[Array[Byte]] = null

  def broadcast(obj: Message) {}

  def allTopicsKey(): String = ""
  def message(context: ChannelHandlerContext, value: Array[Byte]) {}
}
