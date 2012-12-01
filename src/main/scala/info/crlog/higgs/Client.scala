package info.crlog.higgs

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.socket.nio.{NioSocketChannel, NioEventLoopGroup}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.{MessageToByteEncoder, ByteToMessageDecoder}
import java.net.ConnectException
import java.nio.channels.ClosedChannelException
import java.io.IOException
import java.util.concurrent.{LinkedBlockingQueue, LinkedBlockingDeque}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
abstract case class Client[Topic, Msg, SerializedMsg](serviceName: String,
                                                      var port: Int,
                                                      var host: String = "localhost",
                                                      var compress: Boolean = true
                                                       ) extends EventProcessor[Topic, Msg, SerializedMsg] {
  var bootstrap = new Bootstrap()
  var future: ChannelFuture = null
  var channel: Channel = null
  var clientHandler = new ClientHandler[Topic, Msg, SerializedMsg](this)
  var usingCodec = false
  var usingSSL = false
  val SSLclientMode = true
  var connected = false
  val unsentMessages = new LinkedBlockingQueue[Msg]()
  /**
   * If true then should the connection to the server fail the client tries to reconnect
   * until successful at a configurable delay
   */
  var enableAutoReconnect = true
  /**
   * How long should the client wait before attempting to reconnect after a failed connection
   */
  var reconnectTimeout: Long = 2000
  /**
   * If true then when the client reconnects after losing the connection to a remote host
   * it will use a delay between each message that has not yet been sent to the server
   */
  var enablePreEmptiveSend = true
  var preEmptiveTimeout = 200
  var addedReconnectListeners = false
  var blockingConnect = false
  var running = false

  /**
   * Connect to the remote host in preparation for interaction.
   * @param fn function to be called on connected or reconnected
   *           you should never call prepare or listen in the
   */
  def connect(fn: () => Unit = () => {}) {
    bootstrap = new Bootstrap()
    addReconnectListener(fn)
    bootstrap
      .group(new NioEventLoopGroup)
      .channel(classOf[NioSocketChannel])
      .remoteAddress(host, port)
      .handler(new ClientInitializer(this))
    val conFuture = bootstrap.connect()
    channel = conFuture.channel()
    if (usingSSL) {
      //TODO: Fix/Improve SSL handshake waiting
      // Sleep until all the key and trust store configurations loaded, won't work without it
      Thread.sleep(3000)
    }
    conFuture.addListener(new ChannelFutureListener {
      def operationComplete(f: ChannelFuture) {
        future = f
        if (f.isSuccess) {
          connected = true
          fn() //run user on connect callback
        }
      }
    })
    if (blockingConnect) {
      conFuture.awaitUninterruptibly()
      blockingQ.take() //block until something is available
    }
  }

  val blockingQ = new LinkedBlockingDeque[String]()

  /**
   * @return The decoder which decodes message streams
   */
  def decoder(): ByteToMessageDecoder[SerializedMsg]

  /**
   * @return  The encoder to encode messages when sending.
   */
  def encoder(): MessageToByteEncoder[SerializedMsg]


  def handler(ch: SocketChannel) {
    new HiggsChannelPropertyWorkAround(ch, clientHandler)
  }

  ++(Event.CHANNEL_INACTIVE, (ctx: ChannelHandlerContext, x: Option[Throwable]) => {
    connected = false
  })

  /**
   *
   * Starts up a thread via the existing {@link #threadPool}   takes messages off the unset
   * messages queue and send them. If a callback is provided the it will not do this automatically
   * the callback you provide must process unsent messages
   * Called once when a client instance is created.
   * Only call if {@connected} ==false OR you want multiple threads processing the
   * unsent messages queue. Note its unlikely too many threads will yield any real benefit,
   * moderate, 1 or 2 threads should be enough. Any more and the contention rate when reading
   * from the queue will go up thus being counter productive as you're spending potentially more
   * time locking tha you are sending...
   * Also important to note is that this thread just sits in a loop, while(running)
   * as such. if a callback is provided the callback should block when there is nothing for it
   * to do.
   * @param callback  A function to repeatedly execute while the client is running
   */
  def start(callback: () => Any = () => {
    val msg = unsentMessages.take()
    channel.write(serialize(msg))
    channel.flush()
  }) {
    threadPool.submit(new Runnable {
      def run() {
        running = true
        while (running) {
          callback()
        }
        running = false
      }
    })
  }

  //call start once by default
  start()

  /**
   * Set {@connected} to false causing the message processing thread to halt
   * @return
   */
  def stop() = {
    connected = false
    this
  }

  /**
   * Send a message to the server.If the client is not currently connected
   * and auto reconnect is enabled then the message will be queued.
   * If auto reconnect is disabled this will throw an illegal state exception
   * If a message is queued and a connection is later re established the messages
   * will be sent in the order the were received
   * @param msg the message to be sent
   * @tparam T
   * @return   the client to support chaining
   */
  def send[T <: Msg](msg: T): Client[Topic, Msg, SerializedMsg] = {
    if (!connected && !enableAutoReconnect) {
      throw new IllegalStateException("Client is not connected to a server %s and Auto reconnect is disabled." +
        "The message will not be queued as this could lead to out of memory errors due to the unset message backlog" format (serviceName))
    } else {
      unsentMessages.add(msg)
    }
    this
  }

  private def addReconnectListener(fn: () => Unit) {
    if (!addedReconnectListeners) {
      addedReconnectListeners = true
      on(Event.EXCEPTION_CAUGHT, (ctx: ChannelHandlerContext, ex: Option[Throwable]) => {
        var consume = true
        ex match {
          case None => //just here to make match exhaustive, will never happen
          case Some(cause) => {
            cause match {
              case t: ConnectException => {
                Thread.sleep(reconnectTimeout)
                log.warn("Failed to connect to %s on %s:%s, attempting to retry".format(serviceName, host, port))
                connect(fn)
              }
              case cce: ClosedChannelException => {
                log.warn("Client connection to %s on %s:%s socket closed".format(serviceName, host, port))
              }
              case ce: ChannelException => {
                //if connect exception cause the channel exception then don't consume
                if (!ce.getCause.isInstanceOf[ConnectException]) {
                  consume = false
                }
              }
              case ioe: IOException => {
                //io exception can occur because of several things,
                // need a way to detect if the connection was forcibly closed...
                //parsing exeception string is unreliable!
                if (future != null) {
                  Thread.sleep(reconnectTimeout)
                  log.warn("Connection to %s on %s:%s, was forcibly closed, the server may be unavailable, attempting to reconnect".format(serviceName, host, port))
                  connect(fn)
                } else {
                  //if channel future is null then connection not attempted could be another cause to this io ex
                  consume = false
                }
              }
            }
          }
        }
        consume
      })
    }
  }

}
