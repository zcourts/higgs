package info.crlog.higgs.agents.omsg

import commands.Subscribe
import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.compression.{ZlibWrapper, ZlibCodecFactory}
import collection.mutable
import io.netty.logging.{InternalLoggerFactory, InternalLogger}
import java.net.ConnectException
import java.util.concurrent.{Executors, ExecutorService}
import java.nio.channels.ClosedChannelException
import java.io.IOException
import java.io.Serializable
import socket.nio.NioEventLoopGroup

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class OMsgClient(host: String, port: Int) {
  private val log: InternalLogger = InternalLoggerFactory.getInstance(getClass)
  val listeners = mutable.Map.empty[Class[AnyRef], (Any) => Unit]
  var handler = new OMsgClientHandler(listeners)
  var channelfuture: ChannelFuture = null
  var connected = false
  val unsentMessages = mutable.Queue.empty[Serializable]
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

  /**
   * subscribes the given function to any messages of the type provided
   * The client must be connected to subscribe.
   * @param clazz   the type of classes the given function will be sent
   * @param fn the function that will be invoke for each message of the given type that is received
   * @tparam T Any subclass of the base Interaction class
   */
  def listen[T <: Serializable](clazz: Class[T], fn: (T) => Unit) {
    log.info("Subscribing for interactions of type: %s".format(clazz.getName))
    val t = clazz.asInstanceOf[Class[AnyRef]] -> fn.asInstanceOf[(Any) => Unit]
    listeners += t
    //subscribe the client to receive classes of this type
    send(new Subscribe(clazz.getName))
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
  def send[T <: Serializable](msg: T): OMsgClient = {
    if (connected && unsentMessages.isEmpty) {
      handler.send(msg)
    } else {
      if (enableAutoReconnect) {
        enqueueMessage(msg)
      } else {
        throw new IllegalStateException("Client is not connected to a server and Auto reconnect is disabled." +
          "The message will not be queued as this could lead to out of memory errors due to the unset message backlog")
      }
    }
    this
  }

  def enqueueMessage[T <: Serializable](msg: T) = {
    unsentMessages += msg
    if (connected) {
      flushMessageQueue()
    }
  }

  def flushMessageQueue() = {
    threadPool.submit(new Runnable {
      def run() {
        //keep trying to send messages on this thread while the queue is not empty
        while (!unsentMessages.isEmpty) {
          if (connected) {
            send(unsentMessages.dequeue())
            if (enablePreEmptiveSend) {
              Thread.sleep(preEmptiveTimeout)
            }
          } else {
            Thread.sleep(reconnectTimeout)
          }
        }
      }
    })
  }

  val threadPool: ExecutorService = Executors.newCachedThreadPool()

  def newhandler() {
    val exlisteners = handler.exceptionListeners
    handler = new OMsgClientHandler(listeners)
    exlisteners foreach {
      case l => {
        handler.exceptionListeners += l
      }
    }
  }

  def connect(fn: () => Unit) {
    addReconnectListener(fn)
    connect(((f: ChannelFuture) => {
      fn()
    }))
  }


  private def addReconnectListener(fn: () => Unit) {
    handler.addExceptionListener((cause: Throwable) => {
      var consume = true
      cause match {
        case t: ConnectException => {
          Thread.sleep(reconnectTimeout)
          log.warn("Failed to connect to %s:%s, attempting to retry".format(host, port))
          newhandler()
          connect(((f: ChannelFuture) => {
            fn()
          }))
        }
        case cce: ClosedChannelException => {
          log.warn("Client connection socket closed")
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
          if (channelfuture != null) {
            Thread.sleep(reconnectTimeout)
            log.warn("Connection to to %s:%s, was forcibly closed, the server may be unavailable, attempting to reconnect".format(host, port))
            newhandler()
            connect(((f: ChannelFuture) => {
              fn()
            }))
          } else {
            //if channel events is null then connection not attempted could be another cause to this io ex
            consume = false
          }
        }
      }
      consume
    })
  }

  /**
   * Connects to the host:port given at construction.
   * @param onconnected This is a function which is invoked once the client has successfully
   *                    connected. Any client interaction should be done in this function to ensure
   *                    the connection has been established before trying to send messages etc.
   */
  def connect(onconnected: (ChannelFuture) => Unit) {
    threadPool.submit(new Runnable {
      def run() {
        val b: Bootstrap = new Bootstrap
        b.group(new NioEventLoopGroup)
          .channel(classOf[SocketChannel])
          .remoteAddress(host, port)
          .handler(new ChannelInitializer[SocketChannel]() {
          def initChannel(ch: SocketChannel) {
            val pipeline: ChannelPipeline = ch.pipeline
            // Enable stream compression
            pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP))
            pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP))
            // Add the msgpack encoder/decoder
            pipeline.addLast("decoder", new OMsgDecoder)
            pipeline.addLast("encoder", new OMsgEncoder)
            //messaging logic
            pipeline.addLast("handler", handler)
          }
        })
        b.connect().sync().addListener(new ChannelFutureListener {
          def operationComplete(future: ChannelFuture) {
            if (!future.isSuccess) {
              log.error("Failed to connect", future.cause())
            }
            channelfuture = future
            log.info("Connected to server and channel is open:%s".format(future.channel.isOpen))
            connected = true
            onconnected(future)
          }
        })
      }
    })
  }
}