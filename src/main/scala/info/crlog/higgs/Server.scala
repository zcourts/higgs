package info.crlog.higgs

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.socket.nio.{NioEventLoopGroup, NioServerSocketChannel}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.compression.{ZlibWrapper, ZlibCodecFactory}
import io.netty.handler.codec.{MessageToByteEncoder, ByteToMessageDecoder}
import info.crlog.higgs.Event._
import java.util.concurrent.{LinkedBlockingQueue, ConcurrentHashMap}


abstract class Server[T, M, SM](host: String, port: Int, var compress: Boolean = true)
  extends EventProcessor[T, M, SM] {
  val bootstrap: ServerBootstrap = new ServerBootstrap
  var future: ChannelFuture = null
  val handler = new ServerHandler[T, M, SM](this)
  val channels = new ConcurrentHashMap[Int, Channel]()
  var usingSSL = false
  var usingCodec = false
  val SSLclientMode = false
  var bound = false

  /**
   * Bind this server and get the channel it is bound to
   */
  def bind[U](fn: () => U = () => {}): Server[T, M, SM] = {
    bootstrap.group(new NioEventLoopGroup, new NioEventLoopGroup)
      .channel(classOf[NioServerSocketChannel])
      .localAddress(host, port)
      .childHandler(new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) {
        val pipeline: ChannelPipeline = ch.pipeline
        if (usingSSL) {
          //add SSL first if enabled
          ssl(pipeline)
        }
        if (compress) {
          // Enable stream compression
          pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP))
          pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP))
        }
        if (!usingCodec) {
          // Add the encoder/decoder
          pipeline.addLast("decoder", decoder())
          pipeline.addLast("encoder", encoder())
        }
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
        bound = true
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
  def broadcast(obj: M) = {
    val serializedMessage = serializer.serialize(obj)
    val it = channels.keySet().iterator()
    while (it.hasNext) {
      val id = it.next()
      val channel = channels.get(id)
      if (channel != null) {
        channel.write(serializedMessage)
      }
    }
    this
  }

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
  ++(CHANNEL_ACTIVE, (ctx: ChannelHandlerContext, c: Option[Throwable]) => {
    channels.put(ctx.channel().id().toInt, ctx.channel())
  })
  ++(CHANNEL_INACTIVE, (ctx: ChannelHandlerContext, c: Option[Throwable]) => {
    channels.remove(ctx.channel().id().toInt)
  })
}

