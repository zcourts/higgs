package info.crlog.higgs.agents.http

import info.crlog.higgs.{Request, FutureResponse, Client}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelPipeline}
import io.netty.handler.codec.http._
import java.net.{URLEncoder, URL}
import scala.collection.JavaConversions.asJavaIterable
import io.netty.buffer.Unpooled
import io.netty.handler.ssl.SslHandler
import javax.net.ssl.SSLEngine
import info.crlog.higgs.ssl.com.fillta.https.{SSLConfiguration, SSLContextFactory}
import io.netty.handler.stream.ChunkedWriteHandler
import management.ManagementFactory
import java.util.concurrent.Callable
import io.netty.channel.socket.nio.NioEventLoop

/**
 * Courtney Robinson <courtney@crlog.info>
 */

class HttpClient(var USER_AGENT: String = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)")
  extends Client("localhost", 80) {

  def DELETE(url: URL, listener: HTTPEventListener,
             cookies: Map[String, String] = Map.empty[String, String],
             block: Boolean = false,
             httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
             addDefaultHeaders: Boolean = true,
             shutdown: Boolean = true,
             contentType: String = "text/plain",
             gzip: Boolean = false) = {
    val future = makeRequest(listener.response, HttpMethod.DELETE, url, cookies, block, httpVersion,
      addDefaultHeaders, shutdown, Map.empty, contentType, gzip)
    if (block) {
      future.get()
    }
    listener.response
  }

  def GET(url: URL, listener: HTTPEventListener,
          cookies: Map[String, String] = Map.empty[String, String],
          block: Boolean = false,
          httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
          addDefaultHeaders: Boolean = true,
          shutdown: Boolean = true,
          contentType: String = "text/plain",
          gzip: Boolean = false) = {
    val future = makeRequest(listener.response, HttpMethod.GET, url, cookies, block, httpVersion,
      addDefaultHeaders, shutdown, Map.empty, contentType, gzip)
    if (block) {
      future.get()
    }
    listener.response
  }

  def POST(url: URL,data: Map[String, Any]):FutureHTTPResponse={
     POST(url,new HTTPEventListener {
       def onMessage(channel: Channel, msg: String) {}
     },data)
  }

  def POST(url: URL, listener: HTTPEventListener,
           data: Map[String, Any] = Map.empty[String, Any],
           cookies: Map[String, String] = Map.empty[String, String],
           block: Boolean = true,
           httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
           addDefaultHeaders: Boolean = true,
           shutdown: Boolean = true,
           contentType: String = "application/x-www-form-urlencoded",
           gzip: Boolean = false
            ) = {
    val future = makeRequest(listener.response, HttpMethod.POST, url, cookies, block, httpVersion,
      addDefaultHeaders, shutdown, data, contentType, gzip)
    if (block) {
      future.get()
    }
    listener.response
  }

  def PUT(url: URL, listener: HTTPEventListener,
          data: Map[String, Any] = Map.empty[String, Any],
          cookies: Map[String, String] = Map.empty[String, String],
          block: Boolean = true,
          httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
          addDefaultHeaders: Boolean = true,
          shutdown: Boolean = true,
          contentType: String = "application/x-www-form-urlencoded",
          gzip: Boolean = false
           ) = {
    val future = makeRequest(listener.response, HttpMethod.PUT, url, cookies, block, httpVersion,
      addDefaultHeaders, shutdown, data, contentType, gzip)
    if (block) {
      future.get()
    }
    listener.response
  }

  /**
   * <pre>
   * Make an HTTP request to the given URL
   * NOTE: The method is Asynchronous and will return immediately.
   * @param method the HTTP method to use, GET,POST,PUT,DELETE,HEAD
   * @param url the URL to make the request to
   * @param cookies A set of cookies the client should set
   * @param block If TRUE this method will wait until the server closed the connection
   * @param httpVersion The HTTP version to use, default is 1.1
   * @param addDefaultHeaders true if the client should add default headers such as
   *                          host,connection,accept_encoding
   * @param response The response future which will be given the response of this request
   * @param shutdown  If true then the request's bootstrap is shutdown
   *                  </pre>
   */
  def makeRequest(response: FutureHTTPResponse,
                  method: HttpMethod,
                  url: URL,
                  cookies: Map[String, String] = Map.empty[String, String],
                  block: Boolean = true,
                  httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
                  addDefaultHeaders: Boolean = true,
                  shutdown: Boolean = true,
                  data: Map[String, Any] = Map.empty[String, Any],
                  contentType: String = "text/plain",
                  gzip: Boolean = false
                   ) = {
    //use different event loop for every request
    val eventLoop: NioEventLoop = new NioEventLoop()
    eventLoop.unsafe.nextChild.submit(new Callable[Request[AnyRef]] {
      def call() = {
        var ssl = false
        host = url.getHost
        port = if (url.getPort == -1) {
          if (url.toURI.getScheme.equalsIgnoreCase("https")) {
            ssl = true
            443 //ssl
          } else {
            80
          }
        } else {
          url.getPort
        }
        // Prepare the HTTP request.
        val queryString = {
          if (url.getQuery == null) {
            ""
          } else {
            "?" + url.getQuery
          }
        }
        val request = new DefaultHttpRequest(httpVersion, method, url.getPath + queryString)
        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
          val str = new StringBuilder
          data map {
            case (k, v) => {
              if (str.isEmpty) {
                str.append(URLEncoder.encode(k.toString, "UTF-8"))
                  .append("=").
                  append(URLEncoder.encode(v.toString, "UTF-8"))
              } else {
                str.append("&").append(URLEncoder.encode(k.toString, "UTF-8"))
                  .append("=").
                  append(URLEncoder.encode(v.toString, "UTF-8"))
              }
            }
          }
          val encoded = str.mkString
          val encBytes = encoded.getBytes
          val buf = Unpooled.copiedBuffer(encBytes)
          //REQUIRED!!!
          request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, encBytes.length)
          request.setContent(buf)
        }
        if (addDefaultHeaders) {
          request.setHeader(HttpHeaders.Names.HOST, url.getHost)
          request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE)
          request.setHeader(HttpHeaders.Names.USER_AGENT, USER_AGENT)
        }
        if (gzip) {
          request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP)
        }
        request.setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType)
        if (!cookies.isEmpty) {
          val cookieList = for (cookie <- cookies) yield new DefaultCookie(cookie._1, cookie._2)
          request.setHeader(HttpHeaders.Names.COOKIE,
            ClientCookieEncoder.encode(cookieList))
        }
        val handler = new HttpClientHandler(response)
        val req = connect(Some(handler), ssl, gzip, eventLoop) //try to connect

        //before the request has been made but after the request instance has been created
        if (shutdown) {
          req.response ++ ((e: FutureResponse.Event,
                            ctx: ChannelHandlerContext,
                            ex: Option[Throwable]) => {
            if (e == FutureResponse.CHANNEL_UNREGISTERED) {
              //shutdown when response is received
              req.cleanupAndShutdown()
            }
          })
        }
        //now make the request
        req.channel.write(request)
        if (block) {
          // Wait for the server to close the connection.
          req.channel.closeFuture.sync
        }
        req
      }
    })
  }

  //setup http pipeline
  def setupPipeline(ch: SocketChannel, ssl: Boolean, gzip: Boolean) {
    // Create a default pipeline implementation.
    val pipeline: ChannelPipeline = ch.pipeline
    if (ssl) {
      val sslConfiguration: SSLConfiguration = new SSLConfiguration

      import scala.collection.JavaConversions._
      val arg = (for (arg <- ManagementFactory.getRuntimeMXBean().getInputArguments) yield {
        val parts = arg.split('=')
        if (parts.length >= 2)
          parts(0).substring(2) -> parts(1)
        else
          "" -> ""
      }).toMap
      arg.get("javax.net.ssl.keyStore") match {
        case None =>
        case Some(ksPath) => sslConfiguration.setKeyStorePath(ksPath)
      }
      arg.get("javax.net.ssl.keyStorePassword") match {
        case None =>
        case Some(ksPass) => sslConfiguration.setKeyStorePassword(ksPass)
      }
      arg.get("javax.net.ssl.trustStrore") match {
        case None =>
        case Some(tsPath) => sslConfiguration.setTrustStorePath(tsPath)
      }
      arg.get("javax.net.ssl.trustStorePassword") match {
        case None =>
        case Some(tsPass) => sslConfiguration.setTrustStorePassword(tsPass)
      }
      arg.get("javax.net.ssl.keyPassword") match {
        case None =>
        case Some(tsPass) => sslConfiguration.setKeyPassword(tsPass)
      }
      val engine: SSLEngine = SSLContextFactory.getSSLSocket(sslConfiguration).createSSLEngine
      engine.setUseClientMode(true)
      pipeline.addLast("ssl", new SslHandler(engine))
    }
    if (gzip) {
      // Compress
      pipeline.addLast("deflater", new HttpContentCompressor(1))
      // Remove the following line if you don't want automatic content decompression.
      pipeline.addLast("inflater", new HttpContentDecompressor)
    }
    pipeline.addLast("codec", new HttpClientCodec)
    pipeline.addLast("chunked", new ChunkedWriteHandler)
    //    pipeline.addLast("log", new LoggingHandler(LogLevel.INFO))
  }
}
