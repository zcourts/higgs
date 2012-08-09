package info.crlog.higgs

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.socket.SocketChannel

/**
 * Courtney Robinson <courtney@crlog.info>
 */

case class Request[T](bootstrap: Bootstrap,
                   initializer: ChannelInitializer[SocketChannel],
                   channel: Channel,
                   handler: Option[ChannelInboundMessageHandlerAdapter[T]],
                   response: FutureResponse) {
  /**
   * Free up resources
   */
  def cleanupAndShutdown() = {
    channel.closeFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        bootstrap.shutdown()
      }
    })
  }
}
