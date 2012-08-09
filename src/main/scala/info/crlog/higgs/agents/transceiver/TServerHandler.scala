package info.crlog.higgs.agents.transceiver

import io.netty.handler.codec.http.HttpHeaders._
import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.websocketx._
import io.netty.util.CharsetUtil
import info.crlog.higgs.ServerHandler


class TServerHandler() extends ServerHandler[AnyRef] {
  var handshaker: WebSocketServerHandshaker = null

  override def messageReceived(ctx: ChannelHandlerContext, msg: AnyRef) {
    if (msg.isInstanceOf[HttpRequest]) {
      handleHttpRequest(ctx, msg.asInstanceOf[HttpRequest])
    }
    else if (msg.isInstanceOf[WebSocketFrame]) {
      handleWebSocketFrame(ctx, msg.asInstanceOf[WebSocketFrame])
    }
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

  private def handleWebSocketFrame(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    if (frame.isInstanceOf[CloseWebSocketFrame]) {
      handshaker.close(ctx.channel, frame.asInstanceOf[CloseWebSocketFrame])
      return
    }
    else if (frame.isInstanceOf[PingWebSocketFrame]) {
      ctx.channel.write(new PongWebSocketFrame(frame.getBinaryData))
      return
    }
    else if (!(frame.isInstanceOf[TextWebSocketFrame])) {
      throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass.getName))
    }
    val request: String = (frame.asInstanceOf[TextWebSocketFrame]).getText
    if (logger.isDebugEnabled) {
      logger.debug(String.format("Channel %s received %s", ctx.channel.id, request))
    }
    ctx.channel.write(new TextWebSocketFrame(request.toUpperCase))
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
    return "ws://" + req.getHeader(HttpHeaders.Names.HOST)
  }

}


