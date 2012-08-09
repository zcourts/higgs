package info.crlog.higgs.agents.transceiver

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.util.CharsetUtil
import info.crlog.higgs.ClientHandler

class TClientHandler(handshaker: WebSocketClientHandshaker, response: TFuture) extends ClientHandler[AnyRef](response) {

  override def channelInactive(ctx: ChannelHandlerContext) {
    System.out.println("WebSocket Client disconnected!")
  }

  override def messageReceived(ctx: ChannelHandlerContext, msg: AnyRef) {
    val ch: Channel = ctx.channel
    if (!handshaker.isHandshakeComplete) {
      handshaker.finishHandshake(ch, msg.asInstanceOf[HttpResponse])
      System.out.println("WebSocket Client connected!")
      return
    }
    if (msg.isInstanceOf[HttpResponse]) {
      val response: HttpResponse = msg.asInstanceOf[HttpResponse]
      throw new Exception("Unexpected HttpResponse (status=" + response.getStatus + ", content=" + response.getContent.toString(CharsetUtil.UTF_8) + ")")
    }
    val frame: WebSocketFrame = msg.asInstanceOf[WebSocketFrame]
    if (frame.isInstanceOf[TextWebSocketFrame]) {
      val textFrame: TextWebSocketFrame = frame.asInstanceOf[TextWebSocketFrame]
      System.out.println("WebSocket Client received message: " + textFrame.getText)
    }
    else if (frame.isInstanceOf[PongWebSocketFrame]) {
      System.out.println("WebSocket Client received pong")
    }
    else if (frame.isInstanceOf[CloseWebSocketFrame]) {
      System.out.println("WebSocket Client received closing")
      ch.close
    }
  }
}

