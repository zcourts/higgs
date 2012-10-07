package info.crlog.higgs.http

import info.crlog.higgs.{Event, Serializer, Client}
import java.net.{URLEncoder, URL}
import io.netty.channel.{Channel, ChannelHandlerContext}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http._
import io.netty.buffer.{Unpooled, ByteBuf}
import io.netty.util.CharsetUtil
import collection.mutable.ListBuffer

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class HttpRequest(var url: URL,
                       var method: HttpMethod,
                       var cookies: Map[String, String] = Map.empty[String, String],
                       var block: Boolean = false,
                       var httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
                       var addDefaultHeaders: Boolean = true,
                       var shutdown: Boolean = true,
                       var data: Map[String, Any] = Map.empty[String, Any],
                       var contentType: String = "text/plain",
                       var compressionEnabled: Boolean = false,
                       var USER_AGENT: String = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)"
                        )
//host:port ignored
  extends Client[URL, HTTPResponse, AnyRef]("localhost", 80, false) {
  usingCodec = true
  var readingChunks = false

  def send(fn: (HTTPResponse) => Unit) {
    listen(url, (c: Channel, res: HTTPResponse) => {
      fn(res) //send response without channel since responding is not supported
    })
    val uri = url.toURI
    val scheme = if (uri.getScheme == null) "http" else uri.getScheme
    host = if (url.getHost == null) "localhost" else url.getHost
    port = url.getPort
    if (port == -1) {
      if (scheme.equalsIgnoreCase("http")) {
        port = 80
      }
      else if (scheme.equalsIgnoreCase("https")) {
        port = 443
      }
    }
    usingSSL = scheme.equalsIgnoreCase("https")
    // Prepare the HTTP request.
    val queryString = if (url.getQuery == null) "" else "?" + url.getQuery
    val path = if (url.getPath == null) "/" else if (url.getPath.isEmpty) "/" else url.getPath
    val request = new DefaultHttpRequest(httpVersion, method, path + queryString)
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
    if (compressionEnabled) {
      request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP)
    }
    request.setHeader(HttpHeaders.Names.CONTENT_TYPE, contentType)
    if (!cookies.isEmpty) {
      import scala.collection.JavaConversions.asJavaIterable
      val cookieList = for (cookie <- cookies) yield new DefaultCookie(cookie._1, cookie._2)
      request.setHeader(HttpHeaders.Names.COOKIE,
        ClientCookieEncoder.encode(cookieList))
    }
    if (shutdown) {
      ++(Event.CHANNEL_UNREGISTERED, (ctx: ChannelHandlerContext, cause: Option[Throwable]) => {
        bootstrap.shutdown() //listen for channel unregistered event and shutdown
      })
    }
    //finally...connect
    connect(() => {
      //connected now make request
      channel.write(request)
      if (block) {
        // waits if necessary
        future.get()
      }
    })
  }

  override def handler(ch: SocketChannel) {
    // Create a default pipeline implementation.
    val p = ch.pipeline
    // Uncomment the following line if you don't want to handle HttpChunks.
    //pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
    if (compressionEnabled) {
      // Compress
      p.addLast("deflater", new HttpContentCompressor(1))
      // Remove the following line if you don't want automatic content decompression.
      p.addLast("inflater", new HttpContentDecompressor)
    }
    p.addLast("codec", new HttpClientCodec)
    p.addLast("handler", clientHandler)
  }

  val response = new HTTPResponse()

  def createResponse(res: HttpResponse) {
    if (!res.getHeaderNames.isEmpty) {
      import scala.collection.JavaConversions._
      for (name <- res.getHeaderNames) {
        for (value <- res.getHeaders(name)) {
          response.headers.getOrElseUpdate(name, ListBuffer.empty) += value
        }
      }
    }
    response.status = res.getStatus()
    response.protocolVersion = res.getProtocolVersion()
    response.transferEncoding = res.getTransferEncoding()
  }

  def message(ctx: ChannelHandlerContext, msg: AnyRef) {
    if (!readingChunks) {
      val res: HttpResponse = msg.asInstanceOf[HttpResponse]
      createResponse(res)
      if (res.getTransferEncoding.isMultiple) {
        readingChunks = true
      } else {
        val content: ByteBuf = res.getContent
        if (content.readable) {
          //fire message received
          response.data append content.toString(CharsetUtil.UTF_8)
          notifySubscribers(ctx.channel(), url, response)
        }
      }
    } else {
      val chunk: HttpChunk = msg.asInstanceOf[HttpChunk]
      if (chunk.isLast) {
        readingChunks = false
        response.data append chunk.getContent.toString(CharsetUtil.UTF_8)
        //fire message received
        notifySubscribers(ctx.channel(), url, response)
      } else {
        //save chunk
        response.data append chunk.getContent.toString(CharsetUtil.UTF_8)
      }
    }
  }

  //using HTTP codec so serializer,decoder and encoder not required/used
  val serializer: Serializer[HTTPResponse, AnyRef] = null

  def decoder() = null

  def encoder() = null

  def allTopicsKey() = new URL("http://localhost:80/")
}
