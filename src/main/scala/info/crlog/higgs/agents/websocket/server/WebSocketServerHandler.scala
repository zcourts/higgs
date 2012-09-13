package info.crlog.higgs.agents.websocket.server

/**
 * Courtney Robinson <courtney@crlog.info>
 */

import io.netty.handler.codec.http.HttpHeaders._
import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundMessageHandlerAdapter
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.websocketx._
import io.netty.logging.InternalLogger
import io.netty.logging.InternalLoggerFactory
import io.netty.util.CharsetUtil
import collection.mutable
import info.crlog.higgs.agents.msgpack.{Interaction, Packing}
import collection.mutable.ListBuffer
import info.crlog.higgs.agents.msgpack.commands.{Subscribe, Command}

/**
 * Handles handshakes and messages
 */

class WebSocketServerHandler[T <: Interaction](
                                                clients: mutable.Map[Class[Any], ListBuffer[ChannelHandlerContext]],
                                                listeners: mutable.Map[Class[Any], (Any) => Unit]
                                                ) extends ChannelInboundMessageHandlerAdapter[AnyRef] {

  private val log: InternalLogger = InternalLoggerFactory.getInstance(classOf[WebSocketServerHandler[T]])
  private final val WEBSOCKET_PATH: String = ""
  private var handshaker: WebSocketServerHandshaker = null

  def messageReceived(ctx: ChannelHandlerContext, msg: AnyRef) {
    if (msg.isInstanceOf[HttpRequest]) {
      handleHttpRequest(ctx, msg.asInstanceOf[HttpRequest])
    }
    else if (msg.isInstanceOf[WebSocketFrame]) {
      handleWebSocketFrame(ctx, msg.asInstanceOf[WebSocketFrame])
    }
  }

  def unmarshal(data: BinaryWebSocketFrame, ctx: ChannelHandlerContext) {
    val unpacked = Packing.unpack(data)
    val clazz = unpacked._2
    val msg = unpacked._3
    val className = unpacked._1
    msg.asInstanceOf[Interaction].context = ctx
    if (classOf[Command].isAssignableFrom(clazz)) {
      processCommand(className, clazz, msg, ctx)
    } else {
      //get all functions that accept this class
      listeners.get(clazz) match {
        case None => log.warn("Interaction of type %s received but no listeners have been registered for this type".format(className))
        case Some(clz) => {
          clz(msg)
        }
      }
    }
  }

  /**
   * Processes a command. The command may or may not be forwarded to a subscribed function
   * For e.g.. the "Subscribe" command is consumed
   * @param className
   * @param command
   * @param msg
   * @param ctx
   * @return
   */
  protected def processCommand(className: String, command: Class[Any], msg: Any, ctx: ChannelHandlerContext): Any = {
    log.info("Processing command %s".format(className))
    if (classOf[Subscribe].equals(command)) {
      //subscribe this channel to classes of the given type
      clients.
        getOrElseUpdate(
        Class.forName(msg.asInstanceOf[Subscribe].clazz)
          .asInstanceOf[Class[Any]],
        ListBuffer.empty[ChannelHandlerContext]) += ctx
      log.info("Added subscriber for %s".format(msg.asInstanceOf[Subscribe].clazz))
    }
  }

  private def handleWebSocketFrame(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    if (frame.isInstanceOf[CloseWebSocketFrame]) {
      handshaker.close(ctx.channel, frame.asInstanceOf[CloseWebSocketFrame])
      return
    }
    else if (frame.isInstanceOf[PingWebSocketFrame]) {
      ctx.channel.write(new PongWebSocketFrame(frame.getBinaryData))
      return
    }
    else if (!(frame.isInstanceOf[BinaryWebSocketFrame])) {
      throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass.getName))
    }
    unmarshal(frame.asInstanceOf[BinaryWebSocketFrame], ctx)
  }

  private def handleHttpRequest(ctx: ChannelHandlerContext, req: HttpRequest) {
    if (req.getMethod ne GET) {
      sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN))
      return
    }
    val wsFactory: WebSocketServerHandshakerFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false)
    handshaker = wsFactory.newHandshaker(req)
    if (handshaker == null) {
      wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel)
    }
    else {
      handshaker.handshake(ctx.channel, req)
    }
  }


  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace
    ctx.close
  }

  private def sendHttpResponse(ctx: ChannelHandlerContext, req: HttpRequest, res: HttpResponse) {
    if (res.getStatus.getCode != 200) {
      res.setContent(Unpooled.copiedBuffer(res.getStatus.toString, CharsetUtil.UTF_8))
      setContentLength(res, res.getContent.readableBytes)
    }
    val f: ChannelFuture = ctx.channel.write(res)
    if (!isKeepAlive(req) || res.getStatus.getCode != 200) {
      f.addListener(ChannelFutureListener.CLOSE)
    }
  }

  private def getWebSocketLocation(req: HttpRequest): String = {
    return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH
  }

}


