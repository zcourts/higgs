package info.crlog.higgs.agents.websocket.client

/**
 * Courtney Robinson <courtney@crlog.info>
 */

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundMessageHandlerAdapter
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.websocketx._
import io.netty.util.CharsetUtil
import org.msgpack.MessagePack
import collection.mutable
import io.netty.logging.{InternalLoggerFactory, InternalLogger}
import info.crlog.higgs.agents.msgpack.Interaction

class WebSocketClientHandler[T <: Interaction]
(handshaker: WebSocketClientHandshaker, listeners: mutable.Map[Class[Any], (Any) => Unit])
  extends ChannelInboundMessageHandlerAdapter[AnyRef] {
  private final val logger: InternalLogger = InternalLoggerFactory.getInstance(getClass)
  val msgpack = new MessagePack()

  override def channelInactive(ctx: ChannelHandlerContext) {
  }

  def unmarshal(data: BinaryWebSocketFrame, ctx: ChannelHandlerContext) {
    val unpacker = msgpack.createBufferUnpacker()
    unpacker.wrap(data.getBinaryData.array())
    val className = unpacker.readString()
    val clazz: Class[Any] = Class.forName(className).asInstanceOf[Class[Any]]
    msgpack.register(clazz)
    listeners.get(clazz) match {
      case None => logger.warn("Interaction of type %s received but no listeners have been registered for this type".format(className))
      case Some(clz) => {
        val msg = unpacker.read(clazz)
        msg.asInstanceOf[Interaction].context = ctx
        clz(msg)
      }
    }
  }

  def messageReceived(ctx: ChannelHandlerContext, msg: AnyRef) {
    val ch: Channel = ctx.channel
    if (!handshaker.isHandshakeComplete) {
      handshaker.finishHandshake(ch, msg.asInstanceOf[HttpResponse])
      return
    }
    if (msg.isInstanceOf[HttpResponse]) {
      val response: HttpResponse = msg.asInstanceOf[HttpResponse]
      throw new Exception("Unexpected HttpResponse (status=" + response.getStatus + ", content=" + response.getContent.toString(CharsetUtil.UTF_8) + ")")
    }
    val frame: WebSocketFrame = msg.asInstanceOf[WebSocketFrame]
    if (frame.isInstanceOf[BinaryWebSocketFrame]) {
      unmarshal(frame.asInstanceOf[BinaryWebSocketFrame], ctx)
    }
    else if (frame.isInstanceOf[PongWebSocketFrame]) {
      ctx.channel().write(new TextWebSocketFrame(frame.getBinaryData))
    }
    else if (frame.isInstanceOf[CloseWebSocketFrame]) {
      ch.close
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace
    ctx.close
  }

}

