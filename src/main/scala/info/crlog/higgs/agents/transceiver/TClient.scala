package info.crlog.higgs.agents.transceiver

import info.crlog.higgs.Client
import io.netty.channel.socket.SocketChannel
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.{HttpRequestEncoder, HttpResponseDecoder}
import io.netty.handler.codec.http.websocketx.{WebSocketClientHandshaker, WebSocketVersion, WebSocketClientHandshakerFactory}
import java.net.URI
import java.util.HashMap

/**
 * Courtney Robinson <courtney@crlog.info>
 */

class TClient(host: String, port: Int) extends Client(host, port) {
  val headers: HashMap[String, String] = new HashMap[String, String]
  val uri = new URI("ws://" + host + ":" + port)

  val handshaker: WebSocketClientHandshaker = new WebSocketClientHandshakerFactory().newHandshaker(uri, WebSocketVersion.V13, null, false, headers)

  /**
   * Set up the server pipeline. Adding your decode, encoder etc...
   * @param ch  The socket channel to add your handlers to
   * @return  TRUE if and only if you're not adding a "handler" and intend to use
   *          the default handler and its callbacks, FALSE otherwise
   */
  def setupPipeline(ch: SocketChannel, ssl: Boolean, gzip: Boolean) {
    val pipeline: ChannelPipeline = ch.pipeline
    pipeline.addLast("decoder", new HttpResponseDecoder)
    pipeline.addLast("encoder", new HttpRequestEncoder)
    pipeline.addLast("ws-handler", new TClientHandler(handshaker, new TFuture))
  }


  override def connect[T]() = {
    val request = super.connect[T]()
    handshaker.handshake(request.channel).sync
    request
  }
}
