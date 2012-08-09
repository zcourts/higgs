package info.crlog.higgs

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.{NioEventLoop, NioSocketChannel}

abstract class Client(var host: String, var port: Int) {

  /**
   * Don't initalize stuff in the constructor that depends on properties
   * in this abstract class, their state may not be set until just before this
   * method is called. While in this method it is safe to use any property
   * of this super class
   * NOOP by default. Use to override anything that needs to be done before
   * initializing the bootstrap event loop
   */
  def initialize() {}

  /**
   * Set up the server pipeline. Adding your decode, encoder etc...
   * @param ch  The socket channel to add your handlers to
   * @return  TRUE if and only if you're not adding a "handler" and intend to use
   *          the default handler and its callbacks, FALSE otherwise
   */
  def setupPipeline(ch: SocketChannel, ssl: Boolean, gzip: Boolean)

  protected def connect[T](): Request[T] = connect(None)

  protected def connect[T](handler: Option[ClientHandler[T]], ssl: Boolean = false, gzip: Boolean = false,
                           eventLoop: NioEventLoop = new NioEventLoop(),
                           socketChannel: NioSocketChannel = new NioSocketChannel()
                            ): Request[T] = {
    val bootstrap: Bootstrap = new Bootstrap
    var response: FutureResponse = null
    val initializer = new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) {
        setupPipeline(ch, ssl, gzip)
        handler match {
          case None => {
            //assume a handler is provided in setupPipeline
            var handle = ch.pipeline().get("handler")
            if (handle == null) {
              handle = ch.pipeline().get("ws-handler")
            }
            if (handle != null && handle.isInstanceOf[ClientHandler[AnyRef]]) {
              response = handle.asInstanceOf[ClientHandler[T]].future
            } else {
              //so handler is not null and its not a subclass of ClientHandler
              //TODO What to do when handler is not a sub class of ClientHandler
            }
          }
          case Some(h) => {
            ch.pipeline().addLast("handler", h)
            response = h.future
          }
        }
      }
    }
    initialize()
    bootstrap.eventLoop(eventLoop).
      channel(socketChannel).
      remoteAddress(host, port).
      handler(initializer)
    val channel = bootstrap.connect.sync.channel //connect.sync doesn't wait?????
    if (ssl) {
      Thread.sleep(1000) //TODO Look at improving this, sleeping is required to wait for negotiation to complete
    }
    new Request(bootstrap, initializer, channel, handler, response)
  }


}

