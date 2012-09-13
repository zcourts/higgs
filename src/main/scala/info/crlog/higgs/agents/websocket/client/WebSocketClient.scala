package info.crlog.higgs.agents.websocket.client

/**
 * Courtney Robinson <courtney@crlog.info>
 */

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpRequestEncoder
import io.netty.handler.codec.http.HttpResponseDecoder
import io.netty.handler.codec.http.websocketx.{WebSocketClientHandshakerFactory, WebSocketVersion}
import java.util.HashMap
import java.net.URI
import info.crlog.higgs.agents.msgpack.{Interaction, Packing}
import scala.collection._
import io.netty.logging.{Slf4JLoggerFactory, InternalLoggerFactory, InternalLogger}
import info.crlog.higgs.agents.msgpack.commands.Subscribe

class WebSocketClient(host: String, port: Int) {
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
  private val log: InternalLogger = InternalLoggerFactory.getInstance(getClass)
  val listeners = mutable.Map.empty[Class[Any], (Any) => Unit]
  var channel: Channel = null

  /**
   * subscribes the given function to any messages of the type provided
   * The client must be connected to subscribe.
   * @param clazz   the type of classes the given function will be sent
   * @param fn the function that will be invoke for each message of the given type that is received
   * @tparam T Any subclass of the base Interaction class
   */
  def listen[T <: Interaction](clazz: Class[T], fn: (T) => Unit) {
    log.info("Subscribing for interactions of type: %s".format(clazz.getName))
    isConnected()
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
    isConnected
    if (!channel.isOpen) {
      throw new IllegalStateException("Connection to the server is no longer open")
    }
    channel.write(Packing.pack(msg))
    this
  }


  protected def isConnected() {
    if (channel == null) {
      throw new IllegalStateException("This client is not connected")
    }
  }

  def connect(onconnected: (ChannelFuture) => Unit) {
    log.info("Connecting to WebSocket server on %s:%s".format(host, port))
    val b: Bootstrap = new Bootstrap
    val customHeaders: HashMap[String, String] = new HashMap[String, String]
    val handshaker = new WebSocketClientHandshakerFactory()
      .newHandshaker(new URI("ws://" + host + ":" + port + "/"), WebSocketVersion.V13, null, true, customHeaders)
    b.group(new NioEventLoopGroup)
      .channel(new NioSocketChannel)
      .remoteAddress(host, port)
      .handler(new ChannelInitializer[SocketChannel] {
      def initChannel(ch: SocketChannel) {
        val pipeline: ChannelPipeline = ch.pipeline
        pipeline.addLast("decoder", new HttpResponseDecoder)
        pipeline.addLast("encoder", new HttpRequestEncoder)
        pipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker, listeners))
      }
    })
    b.connect().addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          log.error("Failed to connect", future.cause())
        }
        channel = future.channel()
        log.info("Connected to Websocket server and channel is open:%s\nPerforming handshake".format(channel.isOpen))
        handshaker.handshake(channel).addListener(new ChannelFutureListener {
          def operationComplete(future: ChannelFuture) {
            log.info("Handshake complete and channel is open:%s".format(channel.isOpen))
            onconnected(future)
          }
        })
      }
    })
  }
}

