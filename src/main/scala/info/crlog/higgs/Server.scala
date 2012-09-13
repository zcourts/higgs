package info.crlog.higgs

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelInboundMessageHandlerAdapter, ChannelInitializer, Channel}
import io.netty.channel.socket.nio.{NioEventLoopGroup, NioServerSocketChannel}
import io.netty.channel.socket.SocketChannel


abstract class Server(host: String, port: Int) {
  val bootstrap: ServerBootstrap = new ServerBootstrap
  var channel: Option[Channel] = None
  var handler: ChannelInboundMessageHandlerAdapter[AnyRef] = null
  var initializer: ChannelInitializer[SocketChannel] = null

  /**
   * Set up the server pipeline. Adding your decode, encoder etc...
   * @param ch  The socket channel to add your handlers to
   * @return  TRUE if and only if you're not adding a "handler" and intend to use
   *          the default handler and its callbacks, FALSE otherwise
   */
  def setupPipeline(ch: SocketChannel): Boolean

  /**
   * Provide the Server Inbount message handler adapter.
   * Override to supply a custom adapter
   * @return
   */
  def getServerHandler(): ChannelInboundMessageHandlerAdapter[AnyRef] = new ServerHandler

  /**
   * Don't initalize stuff in the constructor that depends on properties
   * in this abstract class, their state may not be set until just before this
   * method is called. While in this method it is safe to use any property
   * of this super class
   * NOOP by default. Use to override anything that needs to be done before
   * initializing the bootstrap event loop
   */
  def initialize() {}

  def init() {
    initializer = new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) {
        if (setupPipeline(ch)) {
          handler = getServerHandler()
          ch.pipeline().addLast("handler", handler)
        }
      }
    }
    initialize()
    bootstrap.group(new NioEventLoopGroup, new NioEventLoopGroup).
      channel(new NioServerSocketChannel).
      localAddress(port).
      childHandler(initializer)
  }

  /**
   * Bind this server and get the channel it is bound to
   */
  def bind(): Server = {
    init()
    channel = Some(bootstrap.bind.sync.channel)
    this
  }
}

