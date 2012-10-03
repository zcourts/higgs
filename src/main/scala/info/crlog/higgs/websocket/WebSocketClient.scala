package info.crlog.higgs.websocket

import io.netty.handler.codec.http.{HttpRequestEncoder, HttpResponseDecoder}
import io.netty.channel.socket.SocketChannel

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class WebSocketClient(host: String, port: Int) extends Client[AnyRef](host, port, false) {

  override def handler(ch: SocketChannel) {
    //need to override to use ws-handler instead of "handler"
    ch.pipeline().addLast("ws-handler", handler)
  }

  def decoder() = new HttpResponseDecoder()

  def encoder() = new HttpRequestEncoder()
}