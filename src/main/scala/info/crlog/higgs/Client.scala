package info.crlog.higgs

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.socket.nio.{NioSocketChannel, NioEventLoopGroup}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.compression.{ZlibWrapper, ZlibCodecFactory}
import io.netty.handler.codec.{MessageToByteEncoder, ByteToMessageDecoder}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
abstract case class Client[Topic, Msg, SerializedMsg](var host: String,
                                                      var port: Int,
                                                      var compress: Boolean = true
                                                       ) extends EventProcessor[Topic, Msg, SerializedMsg] {
  var bootstrap = new Bootstrap()
  var future: ChannelFuture = null
  var channel: Channel = null
  val clientHandler = new ClientHandler[Topic, Msg, SerializedMsg](this)

  def connect(fn: () => Unit) {
    bootstrap
      .group(new NioEventLoopGroup)
      .channel(classOf[NioSocketChannel])
      .remoteAddress(host, port)
      .handler(new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()
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
    channel = bootstrap
      .connect()
      .sync()
      .addListener(new ChannelFutureListener {
      def operationComplete(f: ChannelFuture) {
        future = f
        fn() //run user on connect callback
      }
    }).channel()
  }

  /**
   * @return The decoder which decodes message streams
   */
  def decoder(): ByteToMessageDecoder[SerializedMsg]

  /**
   * @return  The encoder to encode messages when sending.
   */
  def encoder(): MessageToByteEncoder[SerializedMsg]

  def send[M <: Msg](obj: M) {
    channel.write(serialize(obj))
  }


  def handler(ch: SocketChannel) {
    ch.pipeline().addLast("handler", clientHandler)
  }
}
