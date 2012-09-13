package info.crlog.higgs.agents.msgpack

import commands.Subscribe
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.nio.{NioSocketChannel, NioEventLoopGroup}
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.compression.{ZlibWrapper, ZlibCodecFactory}
import collection.mutable
import io.netty.logging.{InternalLoggerFactory, InternalLogger}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class MsgpackClient(host: String, port: Int) {
  private val log: InternalLogger = InternalLoggerFactory.getInstance(getClass)
  val listeners = mutable.Map.empty[Class[Any], (Any) => Unit]
  val handler = new MsgpackClientHandler(listeners)
  val packer = new Packing

  /**
   * subscribes the given function to any messages of the type provided
   * The client must be connected to subscribe.
   * @param clazz   the type of classes the given function will be sent
   * @param fn the function that will be invoke for each message of the given type that is received
   * @tparam T Any subclass of the base Interaction class
   */
  def listen[T <: Interaction](clazz: Class[T], fn: (T) => Unit) {
    log.info("Subscribing for interactions of type: %s".format(clazz.getName))
    val t = clazz.asInstanceOf[Class[Any]] -> fn.asInstanceOf[(Any) => Unit]
    listeners += t
    //subscribe the client to receive classes of this type
    send(new Subscribe(clazz.getName))
  }

  /**
   * Send a message to the server
   * @param msg the message to be sent
   * @tparam T
   * @return   the client to support chaining
   */
  def send[T <: Interaction](msg: T) = {
    handler.send(msg)
    this
  }

  def connect(fn: () => Unit) {
    connect(((f: ChannelFuture) => {
      fn()
    }))
  }

  /**
   * Connects to the host:port given at construction.
   * @param onconnected This is a function which is invoked once the client has successfully
   *                    connected. Any client interaction should be done in this function to ensure
   *                    the connection has been established before trying to send messages etc.
   */
  def connect(onconnected: (ChannelFuture) => Unit) {
    val b: Bootstrap = new Bootstrap
    b.group(new NioEventLoopGroup)
      .channel(new NioSocketChannel)
      .remoteAddress(host, port)
      .handler(new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) {
        val pipeline: ChannelPipeline = ch.pipeline
        // Enable stream compression
        pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP))
        pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP))
        // Add the msgpack encoder/decoder
        pipeline.addLast("decoder", new MsgpackDecoder)
        pipeline.addLast("encoder", new MsgpackEncoder)
        //messaging logic
        pipeline.addLast("handler", handler)
      }
    })
    b.connect().sync().addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          log.error("Failed to connect", future.cause())
        }
        log.info("Connected to server and channel is open:%s".format(future.channel.isOpen))
        onconnected(future)
      }
    })
  }
}