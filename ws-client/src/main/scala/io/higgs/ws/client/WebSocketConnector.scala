package io.higgs.ws.client

import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler
import io.netty.handler.codec.http.websocketx.{WebSocketClientHandshakerFactory, WebSocketVersion}
import io.netty.handler.codec.http.{DefaultHttpHeaders, HttpClientCodec, HttpHeaders, HttpObjectAggregator}
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContext, SslContextBuilder}
import io.netty.util.concurrent.GenericFutureListener

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
trait WebSocketConnector {
  processor: WebSocketConnectorEventProcessor =>
  protected def uri: URI

  def group: EventLoopGroup

  def channelClass: Class[_ <: Channel] = classOf[NioSocketChannel]

  //  type F = _ <: Future[_ >: Void]

  lazy val (host, port, ssl) = {
    val scheme = if (uri.getScheme == null) "ws" else uri.getScheme
    val host = if (uri.getHost == null) "127.0.0.1" else uri.getHost
    val port: Int = uri.getPort match {
      case -1 if "ws".equalsIgnoreCase(scheme) => 80
      case -1 if "wss".equalsIgnoreCase(scheme) => 443
      case -1 => throw new UnsupportedOperationException(s"Unsupported scheme AND no port given in $uri.")
      case p => p
    }
    val ssl = scheme match {
      case "ws" => false
      case "wss" => true
      case _ => throw new UnsupportedOperationException(s"$scheme is not a supported URI scheme, use ws or wss")
    }
    (host, port, ssl)
  }
  protected lazy val ctx: Option[SslContext] = ssl match {
    case false => None
    case _ =>
      log.warn("You've not provided an SSL Context, an insecure one which accepts any certificate is being used")
      Some(SslContextBuilder.forClient.trustManager(InsecureTrustManagerFactory.INSTANCE).build)
  }
  protected lazy val channel = new AtomicReference[Channel]()
  protected lazy val disconnected = new AtomicReference[Boolean](false)

  def disconnect(): Unit = {
    disconnected.set(true)
    val ch = channel.get()
    if (ch != null) {
      ch.close()
    }
  }

  protected def connect(
                         reconnectTimeout: Int = 1,
                         subProtocol: String = null,
                         allowExtensions: Boolean = true,
                         headers: HttpHeaders = new DefaultHttpHeaders,
                         maxFramePayloadLength: Int = 65536,
                         maxContentLength: Int = 8192,
                         performMasking: Boolean = true,
                         allowMaskingMismatch: Boolean = false,
                         version: WebSocketVersion = WebSocketVersion.V13): Unit = {

    val handler = new WebSocketClientHandler(WebSocketClientHandshakerFactory
      .newHandshaker(uri, version, subProtocol, allowExtensions, headers,
        maxFramePayloadLength, performMasking, allowMaskingMismatch), this, processor)

    val b: Bootstrap = new Bootstrap
    b.group(group).channel(channelClass).handler(new ChannelInitializer[SocketChannel]() {
      protected def initChannel(ch: SocketChannel) {
        val p: ChannelPipeline = ch.pipeline
        ctx map (s => p.addLast(s.newHandler(ch.alloc, host, port)))
        p.addLast(
          new HttpClientCodec,
          new HttpObjectAggregator(maxContentLength),
          WebSocketClientCompressionHandler.INSTANCE,
          handler
        )
      }
    })
    //    val chan = b.connect(uri.getHost, port).sync().channel()
    //    handler.future.sync()
    //    chan.writeAndFlush(new TextWebSocketFrame("test"))
    val connectFuture = b.connect(uri.getHost, port)
    connectFuture.addListener(new GenericFutureListener[ChannelFuture] {
      override def operationComplete(future: ChannelFuture): Unit = {
        if (future.isSuccess) {
          channel.set(connectFuture.channel())
          connectFuture.channel().closeFuture().addListener(new GenericFutureListener[ChannelFuture] {
            override def operationComplete(future: ChannelFuture): Unit = {
              if (reconnectTimeout > 0 && !disconnected.get()) {
                connect(reconnectTimeout, subProtocol, allowExtensions, headers, maxFramePayloadLength, maxContentLength,
                  performMasking, allowMaskingMismatch, version)
              }
            }
          })
          //handler.sync
        } else {
          log.warn(s"Connecting to $uri failed${if (future.cause() != null) ", " + future.cause().getMessage}, scheduling retry", future.cause())
          group.schedule(new Runnable {
            override def run(): Unit = {
              val nextTimeout = reconnectTimeout * 2
              connect(if (nextTimeout > 30) 30 else nextTimeout, subProtocol, allowExtensions, headers,
                maxFramePayloadLength, maxContentLength, performMasking, allowMaskingMismatch, version)
            }
          }, reconnectTimeout, TimeUnit.SECONDS)
        }
      }
    })
  }
}
