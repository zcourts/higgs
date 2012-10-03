package info.crlog.higgs

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.socket.nio.{NioEventLoopGroup, NioServerSocketChannel}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.compression.{ZlibWrapper, ZlibCodecFactory}
import io.netty.handler.codec.{MessageToByteEncoder, ByteToMessageDecoder}
import info.crlog.higgs.Event._
import collection.mutable
import collection.mutable.ListBuffer


abstract class Server[T, M, SM](host: String, port: Int, var compress: Boolean = true)
  extends EventProcessor[T, M, SM] {
  val bootstrap: ServerBootstrap = new ServerBootstrap
  var future: ChannelFuture = null
  val handler = new ServerHandler[T, M, SM](this)
  val channels = mutable.Map.empty[Int, Channel]

  /**
   * Bind this server and get the channel it is bound to
   */
  def bind(fn: () => Unit): Server[T, M, SM] = {
    bootstrap.group(new NioEventLoopGroup, new NioEventLoopGroup)
      .channel(classOf[NioServerSocketChannel])
      .localAddress(host, port)
      .childHandler(new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) {
        val pipeline: ChannelPipeline = ch.pipeline
        if (compress) {
          // Enable stream compression
          pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP))
          pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP))
        }
        // Add the encoder/decoder
        pipeline.addLast("decoder", decoder())
        pipeline.addLast("encoder", encoder())
        //messaging logic
        handler(ch)
      }
    })
    bootstrap
      .bind()
      .sync()
      .addListener(new ChannelFutureListener {
      def operationComplete(f: ChannelFuture) {
        future = f
        fn() //run user on connect callback
      }
    })
    this
  }

  /**
   * @return The decoder which decodes message streams
   */
  def decoder(): ByteToMessageDecoder[SM]

  /**
   * @return  The encoder to encode messages when sending.
   */
  def encoder(): MessageToByteEncoder[SM]

  def handler(ch: SocketChannel) {
    ch.pipeline().addLast("handler", handler)
  }

  //  def allTopicsKey(): T

  /**
   * Send a message to ALL connected clients
   * @param obj the message to send. This will be passed to serializer.serialize
   * @return  Server
   */
  def broadcast(obj: M)

  //  /**
  //   * Add a function to be invoked when a message is received
  //   * @param fn
  //   */
  //  def ++(fn: (Channel, M) => Unit)
  //
  //  /**
  //   * Subscribes the given function to ALL messages received regardless of the message's
  //   * topic
  //   * @param fn the function to subscribe
  //   */
  //  def listen(fn: (Channel, M) => Unit)
  //
  //  /**
  //   * Subscribes the given function to the given topic
  //   * @param topic  The topic to subscribe to, if this is an empty string the function will receive ALL messages
  //   * @param fn The function to be subscribed to the given topic
  //   */
  //  def listen(topic: T, fn: (Channel, M) => Unit)


  //capture channel contexts when active  and remove them when inactive
  ++((event: Event, ctx: ChannelHandlerContext, c: Option[Throwable]) => {
    event match {
      case CHANNEL_ACTIVE => {
        channels += ctx.channel().id().toInt -> ctx.channel()
      }
      case CHANNEL_INACTIVE => {
        channels -= ctx.channel().id().toInt
      }
      case _ =>
    }
  })

}

