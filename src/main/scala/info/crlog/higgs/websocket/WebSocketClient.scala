package info.crlog.higgs.websocket

import io.netty.handler.codec.http.{HttpResponse, HttpRequestEncoder, HttpResponseDecoder}
import io.netty.channel.socket.SocketChannel
import info.crlog.higgs.messages.JsonMessage
import info.crlog.higgs.serializers.JsonSerializer
import io.netty.channel.{ChannelHandlerContext, ChannelFuture, ChannelFutureListener, Channel}
import io.netty.handler.codec.http.websocketx._
import java.net.URL
import java.util
import io.netty.util.CharsetUtil
import info.crlog.higgs.Client

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class WebSocketClient(host: String, port: Int)
  extends Client[String, JsonMessage, AnyRef](host, port, false) {
  val serializer = new JsonSerializer()
  val handshaker = new WebSocketClientHandshakerFactory()
    .newHandshaker(new URL("http://" + host + ":" + port + "/").toURI, WebSocketVersion.V13, null,
    false, new util.HashMap[String, String]())

  override def handler(ch: SocketChannel) {
    //need to override to use ws-handler instead of "handler"
    val pipeline = ch.pipeline()
    pipeline.addLast("ws-handler", clientHandler)
  }

  override def connect(fn: () => Unit) {
    super.connect(() => {
      handshaker.handshake(channel).sync().addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          fn()
        }
      })
    })
  }

  override def message(ctx: ChannelHandlerContext, msg: AnyRef) {
    val ch: Channel = ctx.channel
    if (!handshaker.isHandshakeComplete()) {
      handshaker.finishHandshake(ch, msg.asInstanceOf[HttpResponse])
      return
    }
    if (msg.isInstanceOf[HttpResponse]) {
      val response = msg.asInstanceOf[HttpResponse]
      throw new Exception("Unexpected HttpResponse (status=" + response.getStatus + ", content=" + response.getContent.toString(CharsetUtil.UTF_8) + ")")
    }
    val frame = msg.asInstanceOf[WebSocketFrame]
    if (frame.isInstanceOf[TextWebSocketFrame]) {
      //fire off message received
      val msg = serializer.deserialize(frame.asInstanceOf[TextWebSocketFrame].getText())
      notifySubscribers(ctx.channel(), msg.topic, msg)
    } else if (frame.isInstanceOf[PongWebSocketFrame]) {
    } else if (frame.isInstanceOf[CloseWebSocketFrame]) {
      ch.close
    }
  }

  def decoder() = new HttpResponseDecoder()

  def encoder() = new HttpRequestEncoder()

  def allTopicsKey() = ""

}