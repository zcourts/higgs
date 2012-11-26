package info.crlog.higgs.protocols.websocket

import info.crlog.higgs.Server
import io.netty.handler.codec.http._
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelFutureListener, ChannelFuture, ChannelHandlerContext}
import websocketx._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpMethod._
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpHeaders._
import io.netty.util.CharsetUtil

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class WebSocketServer(host: String, port: Int) extends Server[String, JsonMessage, AnyRef](host, port, false) {
  val serializer = new JsonSerializer()
  var WEBSOCKET_PATH = "/"
  var handshaker: WebSocketServerHandshaker = null

  override def handler(ch: SocketChannel) {
    ch.pipeline().addLast("aggregator", new HttpChunkAggregator(65536))
    super.handler(ch) //add handler last
  }

  override def message(ctx: ChannelHandlerContext, msg: AnyRef) {
    //handle http request and handshakes as necessary
    if (msg.isInstanceOf[HttpRequest]) {
      handleHttpRequest(ctx, msg.asInstanceOf[HttpRequest])
    }
    else if (msg.isInstanceOf[WebSocketFrame]) {
      val frame = msg.asInstanceOf[WebSocketFrame]
      handleWebSocketFrame(ctx, frame)
    }
  }

  private def handleHttpRequest(ctx: ChannelHandlerContext, req: HttpRequest) {
    if (!req.getDecoderResult.isSuccess) {
      sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, BAD_REQUEST))
      return
    }
    //TODO check if method is POST, if it is assume client doesn't support WebSocket and is polling
    if (req.getMethod != GET) {
      sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED))
      return
    } else if (req.getUri == "/favicon.ico") {
      val res: HttpResponse = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
      sendHttpResponse(ctx, req, res)
      return
    }
    val wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false)
    handshaker = wsFactory.newHandshaker(req)
    if (handshaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel)
    } else {
      handshaker.handshake(ctx.channel, req)
    }
  }

  private def handleWebSocketFrame(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    if (frame.isInstanceOf[CloseWebSocketFrame]) {
      handshaker.close(ctx.channel, frame.asInstanceOf[CloseWebSocketFrame])
      return
    } else if (frame.isInstanceOf[PingWebSocketFrame]) {
      ctx.channel.write(new PongWebSocketFrame(frame.getBinaryData))
      return
    } else if (!(frame.isInstanceOf[TextWebSocketFrame])) {
      throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass.getName))
    }
    //now fire off de-serialization etc
    val msg = serializer.deserialize(frame.asInstanceOf[TextWebSocketFrame].getText())
    notifySubscribers(ctx.channel(), msg.topic, msg)
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

  def decoder() = new HttpRequestDecoder()

  def encoder() = new HttpResponseEncoder()

  //  ++((c: Channel, m: JsonMessage) => {
  //    m.channel = c
  //    m.serializer = serializer
  //    notifySubscribers(c, m.topic, m)
  //  })

  def allTopicsKey() = "" //any subscribers without a topic goes uses this
}
