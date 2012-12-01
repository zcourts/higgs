package info.crlog.higgs.protocols.http

import java.net.{URI, InetSocketAddress}
import io.netty.channel._
import io.netty.handler.codec.http._
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.nio.{NioSocketChannel, NioEventLoopGroup}
import io.netty.util.{CharsetUtil, AttributeKey}
import io.netty.buffer.ByteBuf
import collection.mutable.ListBuffer
import multipart._
import info.crlog.higgs.Client
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class RequestProcessor extends Client[String, HTTPResponse, AnyRef]("HttpClient", 80, "localhost", false) {
  private val messageQ = new LinkedBlockingQueue[() => Unit]()
  val attReq = new AttributeKey[HttpRequestBuilder]("request")
  val attChunks = new AttributeKey[Boolean]("chunks")
  val attResponse = new AttributeKey[HTTPResponse]("response")
  val attTopic = new AttributeKey[String]("topic")

  def message(ctx: ChannelHandlerContext, msg: AnyRef) {
    //get the request that this message is in response to
    val req = ctx.channel().attr(attReq).get()
    val readingChunks = ctx.channel().attr(attChunks).get()
    val resAttr = ctx.channel().attr(attResponse)
    val response = if (resAttr.get() == null) {
      resAttr.set(new HTTPResponse())
      resAttr.get()
    } else {
      resAttr.get()
    }
    val requestID = ctx.channel().attr(attTopic).get()
    if (!readingChunks) {
      val res: HttpResponse = msg.asInstanceOf[HttpResponse]
      if (!res.getHeaderNames.isEmpty) {
        val it = res.getHeaderNames().iterator()
        while (it.hasNext()) {
          val name = it.next()
          val hit = res.getHeaders(name).iterator()
          while (hit.hasNext()) {
            val value = hit.next()
            response.headers.getOrElseUpdate(name, ListBuffer.empty) += value
          }
        }
      }
      response.status = res.getStatus()
      response.protocolVersion = res.getProtocolVersion()
      response.transferEncoding = res.getTransferEncoding()
      if (res.getTransferEncoding.isMultiple) {
        ctx.channel().attr(attChunks).set(true)
      } else {
        val content: ByteBuf = res.getContent
        if (content.readable) {
          //fire message received
          response.data append content.toString(CharsetUtil.UTF_8)
          notifySubscribers(ctx.channel(), requestID, response)
        }
      }
    } else {
      val chunk: HttpChunk = msg.asInstanceOf[HttpChunk]
      if (chunk.isLast) {
        ctx.channel().attr(attChunks).set(false)
        response.data append chunk.getContent.toString(CharsetUtil.UTF_8)
        //fire message received
        notifySubscribers(ctx.channel(), requestID, response)
      } else {
        //save chunk
        response.data append chunk.getContent.toString(CharsetUtil.UTF_8)
      }
    }
  }

  def getOrDelete[U](req: HttpRequestBuilder, responseListener: (HTTPResponse) => U) {
    //get requests are simple and only require this
    val request = createRequest(req)
    request.setMethod(req.requestMethod)
    val send = () => {
      val bootstrap = newBootstrap(req)
      val conFuture = bootstrap.connect
      conFuture.sync
      val channel: Channel = conFuture.channel
      //try to use a unique ID as opposed to just the URL because multiple request can go to the
      // same URLwith different callbacks which could cause the wrong callback to be invoked
      val id = req.url().toString + System.nanoTime()
      channel.attr(attTopic).set(id)
      //listen for response
      listen(id, (ch, res) => {
        responseListener(res)
      })
      val l = conFuture.addListener(new ChannelFutureListener {
        def operationComplete(f: ChannelFuture) {
          if (f.isSuccess) {
            channel.attr(attReq).set(req)
            channel.write(request)
            //TODO causes BlockingOpEx
            //channel.closeFuture.sync
          }
        }
      })
    }
    messageQ.add(send)
  }

  def postOrPut[U](req: HttpRequestBuilder, responseListener: (HTTPResponse) => U) {
    //get requests are simple and only require this
    val request = createRequest(req)
    request.setMethod(req.requestMethod)
    // setup the factory: here using a mixed memory/disk based on size threshold
    val factory: HttpDataFactory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE)
    //if user explicitly sets multi-part to false then only file name is sent otherwise
    //if at least 1 file is supplied it is multi-part
    val multiPart = req.multiPart && (req.formMultiFiles.size > 0 || req.formFiles.size > 0)
    //create a new POST encoder, if files are to be uploaded make it a multipart form (last param)
    val encoder = new HttpPostRequestEncoder(factory, request, multiPart)
    //add form params
    req.formParameters.foreach((kv) => {
      val name = kv._1
      val value = kv._2
      encoder.addBodyAttribute(name, value.toString())
    })
    //add form Files, if any
    req.formFiles.foreach((file) => {
      encoder.addBodyFileUpload(file.name, file.file, file.contentType, file.isText)
    })
    //add multiple files under the same name
    req.formMultiFiles.foreach((kv) => {
      val name = kv._1
      val files = kv._2
      val arrFiles = new Array[File](files.size)
      val arrContentType = new Array[String](files.size)
      val arrIsText = new Array[Boolean](files.size)

      for (i <- 0 until files.size) {
        arrFiles(i) = files(i).file
        arrContentType(i) = files(i).contentType
        arrIsText(i) = files(i).isText
      }
      encoder.addBodyFileUploads(name, arrFiles, arrContentType, arrIsText)
    })
    try {
      //encode
      encoder.finalizeRequest()
      val send = () => {
        val bootstrap = newBootstrap(req)
        val conFuture = bootstrap.connect
        conFuture.sync
        val channel: Channel = conFuture.channel
        //try to use a unique ID as opposed to just the URL because multiple request can go to the
        // same URLwith different callbacks which could cause the wrong callback to be invoked
        val id = req.url().toString + System.nanoTime()
        channel.attr(attTopic).set(id)
        //listen for response
        listen(id, (ch, res) => {
          responseListener(res)
        })
        val l = conFuture.addListener(new ChannelFutureListener {
          def operationComplete(f: ChannelFuture) {
            if (f.isSuccess) {
              channel.attr(attReq).set(req)
              channel.write(request)
              // test if request was chunked and if so, finish the write
              if (encoder.isChunked()) {
                channel.write(encoder) //.awaitUninterruptibly
              }
              //TODO causes BlockingOpEx
              //channel.closeFuture.sync
            }
          }
        })
      }
      messageQ.add(send)
    } catch {
      case e => {
        log.error("Unable to send %s request and exception occurred." +
          " Perhaps you created a malformed or incomplete request" format (req.requestMethod), e)
      }
    }
    encoder.cleanFiles()
  }

  //stop default message processing thread
  stop()
  //provide custom message processing function
  start(() => {
    val fn = messageQ.take()
    fn()
  })

  /**
   * Create an {@link HttpRequest} with {@link HttpHeaders} including (cookies, user agent etc),
   * and query string parameters set
   * @param req
   * @tparam U
   * @return
   */
  def createRequest[U](req: HttpRequestBuilder): DefaultHttpRequest = {
    val encoder: QueryStringEncoder = new QueryStringEncoder(req.path())
    req.urlParameters.foreach((kv) => {
      encoder.addParam(kv._1, kv._2.toString())
    })
    val uriGet = new URI(encoder.toString)
    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uriGet.toASCIIString)
    if (req.addDefaultHeaders) {
      request.setHeader(HttpHeaders.Names.HOST, req.url().getHost())
      request.setHeader(HttpHeaders.Names.CONNECTION, req.header_connection_value)
      request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, req.header_accept_encoding)
      request.setHeader(HttpHeaders.Names.ACCEPT_CHARSET, req.header_accept_charset)
      request.setHeader(HttpHeaders.Names.ACCEPT_LANGUAGE, req.header_accept_lang)
      request.setHeader(HttpHeaders.Names.REFERER, req.url().toString)
      request.setHeader(HttpHeaders.Names.USER_AGENT, req.USER_AGENT)
      request.setHeader(HttpHeaders.Names.ACCEPT, req.requestContentType)
    }
    req.requestHeaders.foreach((kv) => {
      request.setHeader(kv._1, kv._2)
    })
    val cookieList = for (cookie <- req.requestCookies) yield new DefaultCookie(cookie._1, cookie._2)
    //set cookies
    request.setHeader(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(cookieList.toSeq: _*))
    request
  }

  def newBootstrap(req: HttpRequestBuilder): Bootstrap = {
    val bootstrap = new Bootstrap()
    val uri = req.url().toURI()
    val scheme = if (uri.getScheme == null) "http" else uri.getScheme
    val reqhost = if (req.url.getHost == null) "localhost" else req.url.getHost
    var reqport = req.url.getPort
    if (reqport == -1) {
      if (scheme.equalsIgnoreCase("http")) {
        reqport = 80
      } else if (scheme.equalsIgnoreCase("https")) {
        reqport = 443
      }
    }
    req.useSSL = scheme.equalsIgnoreCase("https")
    //not using ClientInitializer because it doesn't accept an HTTP request builder
    bootstrap.group(new NioEventLoopGroup()).channel(classOf[NioSocketChannel])
      .handler(new HttpClientInitializer(req, this))
    bootstrap.remoteAddress(new InetSocketAddress(reqhost, reqport))
  }

  //using HTTP codec so serializer,decoder and encoder not required/used
  def decoder() = null

  def encoder() = null

  val serializer = null

  def allTopicsKey() = ""
}
