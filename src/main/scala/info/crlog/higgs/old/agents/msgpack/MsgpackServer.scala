package info.crlog.higgs.agents.msgpack

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioEventLoopGroup}
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.compression.{ZlibWrapper, ZlibCodecFactory}
import io.netty.logging.{InternalLoggerFactory, InternalLogger}
import collection.mutable
import collection.mutable.ListBuffer

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class MsgpackServer(host: String, port: Int) {
  private val log: InternalLogger = InternalLoggerFactory.getInstance(getClass)
  /**
   * A list of classes and a list contexts (inc channel) that are to be
   * sent broadcasts of that class
   */
  val clients = mutable.Map.empty[Class[Any], ListBuffer[ChannelHandlerContext]]
  /**
   * A set of functions that the web socket server will invoke when
   * messages of a given class are received.
   */
  val listeners = mutable.Map.empty[Class[Any], (Any) => Unit]
  val packer = new Packing
  val bootstrap: ServerBootstrap = new ServerBootstrap

  /**
   * Listen to messages from all connected clients.
   * @param clazz
   * @param fn
   * @tparam T
   */
  def listen[T <: Interaction](clazz: Class[T], fn: (T) => Unit) {
    log.info("Adding listener for classes of type %s".format(clazz.getName))
    val t = clazz.asInstanceOf[Class[Any]] -> fn.asInstanceOf[(Any) => Unit]
    listeners += t
  }

  def broadcast[T <: Interaction](msg: T) {
    clients get (msg.getClass.asInstanceOf[Class[Any]]) match {
      case None => log.warn("No subscribers for this class type (%s)".format(msg.getClass.getName))
      case Some(subscribers) => {
        //create serialized message outside of the loop
        val frame = packer.packBytes(msg)
        subscribers.foreach {
          case ctx => {
            if (ctx.channel().isOpen) {
              //write the same frame to all subscribed clients
              ctx.channel.write(frame)
            } else {
              subscribers -= ctx
              log.warn("channel found matching subscription but the channel is not open, its been removed")
            }
          }
        }
      }
    }
  }

  def bind(fn: () => Unit) {
    bind((f: ChannelFuture) => {
      fn()
    })
  }

  def bind(onbound: (ChannelFuture) => Unit) {
    bootstrap.group(new NioEventLoopGroup, new NioEventLoopGroup)
      .channel(classOf[NioServerSocketChannel])
      .localAddress(host, port)
      .childHandler(new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) {
        val pipeline: ChannelPipeline = ch.pipeline
        // Enable stream compression
        pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP))
        pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP))
        // Add the msgpack encoder/decoder
        pipeline.addLast("decoder", new MsgpackDecoder)
        pipeline.addLast("encoder", new MsgpackEncoder)
        //messaging logic
        pipeline.addLast("handler", new MsgpackServerHandler(clients, listeners))
      }
    })
    bootstrap.bind().sync().addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          log.error("Failed to connect", future.cause())
        }
        log.info("Connected to server and channel is open:%s".format(future.channel.isOpen))
        onbound(future)
      }
    })
  }
}

