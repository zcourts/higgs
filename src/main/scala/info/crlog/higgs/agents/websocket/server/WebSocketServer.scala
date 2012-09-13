package info.crlog.higgs.agents.websocket.server


import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelHandlerContext, Channel, ChannelInitializer, ChannelPipeline}
import io.netty.channel.socket.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.{HttpResponseEncoder, HttpChunkAggregator, HttpRequestDecoder}
import io.netty.channel.socket.SocketChannel
import info.crlog.higgs.agents.msgpack.{Interaction, Packing}
import collection.mutable
import collection.mutable.ListBuffer
import org.msgpack.MessagePack
import io.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory, InternalLogger}


/**
 * When a client connects they need to send a subscription message.
 * This message is simply the fully qualified name of a class for which
 * that client wants to receive messages for.
 * When an instance of any class is sent via the transceiver it only
 * sends that message to clients that have subscribed to that class
 * Courtney Robinson <courtney@crlog.info>
 */

class WebSocketServer(port: Int) {
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
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
  val msgpack = new MessagePack()

  var channel: Channel = null

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
        val frame = Packing.pack(msg)
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

  def bind() {
    log.info("Binding to  port %s".format(port))
    val b: ServerBootstrap = new ServerBootstrap
    b.group(new NioEventLoopGroup, new NioEventLoopGroup)
      .channel(new NioServerSocketChannel)
      .localAddress(port)
      .childHandler(new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) {
        val pipeline: ChannelPipeline = ch.pipeline
        pipeline.addLast("decoder", new HttpRequestDecoder)
        pipeline.addLast("aggregator", new HttpChunkAggregator(65536))
        pipeline.addLast("encoder", new HttpResponseEncoder)
        pipeline.addLast("handler", new WebSocketServerHandler(clients, listeners))
      }
    })
    channel = b.bind.sync.channel
    log.info("Server channel open :" + channel.isOpen)
  }
}

