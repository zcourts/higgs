package info.crlog.higgs.ssl

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http._
import java.net.URI
import java.net.URISyntaxException

object SSLClient {
  def main(args: Array[String]) {
    val sslClient: SSLClient = new SSLClient
    val sslConfiguration: SSLConfiguration = new SSLConfiguration
    //    sslConfiguration.setTrustStorePath("B:\\Courtney\\Desktop\\NettyHTTPS\\src\\main\\java\\keystores\\truststore.ks")
    //    sslConfiguration.setTrustStorePassword("zcourts")
    //    sslConfiguration.setKeyStorePath("B:\\Courtney\\Desktop\\NettyHTTPS\\src\\main\\java\\keystores\\keystore.ks")
    //    sslConfiguration.setKeyStorePassword("zcourts")
    //    sslConfiguration.setKeyPassword("zcourts")
    sslClient.setUrl("https://graph.facebook.com/me/feed?access_token=AAAC9iVp3fpoBAGuVHs63PfduHzKrZAMC88CavXOjTGKXFfIDZB76hXVWLlu48IZBZAVZAkELNdNQARBTv4w3hRs2sswWX5AV6maiCgzVC8QZDZD")
    sslClient.setSslConfiguration(sslConfiguration)
    try {
      sslClient.execute
    }
    catch {
      case e: Exception => {
        e.printStackTrace
      }
    }
  }
}

class SSLClient {
  def setSslConfiguration(sslConfiguration: SSLConfiguration) {
    this.sslConfiguration = sslConfiguration
  }

  def setUrl(urlString: String) {
    this.urlString = urlString
  }

  def parseArgs(args: Array[String]) {
    if (args.length / 2 != 0) {
      System.out.println("Number of arguments passed should be multiple of 2")
      throw new Exception("Number of arguments passed should be multiple of 2")
    }
    {
      var i: Int = 0
      while (i < args.length) {
        {
          sslConfiguration = new SSLConfiguration
          val paramName: String = args(i)
          if ("-url".equalsIgnoreCase(paramName.trim)) {
            urlString = args(({
              i += 1;
              i - 1
            }))
          }
          else if ("-keystore".equalsIgnoreCase(paramName.trim)) {
            sslConfiguration.setKeyStorePath(args(({
              i += 1;
              i - 1
            })).trim)
          }
          else if ("-keypassword".equalsIgnoreCase(paramName.trim)) {
            sslConfiguration.setKeyPassword(args(({
              i += 1;
              i - 1
            })).trim)
          }
          else if ("-keystorepassword".equalsIgnoreCase(paramName.trim)) {
            sslConfiguration.setKeyStorePassword(args(({
              i += 1;
              i - 1
            })).trim)
          }
          else if ("-truststore".equalsIgnoreCase(paramName.trim)) {
            sslConfiguration.setTrustStorePassword(args(({
              i += 1;
              i - 1
            })).trim)
          }
          else if ("-truststorepassword".equalsIgnoreCase(paramName.trim)) {
            sslConfiguration.setTrustStorePassword(args(({
              i += 1;
              i - 1
            })).trim)
          }
        }
        ({
          i += 1;
          i
        })
      }
    }
  }

  def execute {
    var uri: URI = null
    try {
      uri = new URI(urlString)
    }
    catch {
      case e: URISyntaxException => {
        System.out.println("Exception while parsing the url provided. Reason: " + e.getLocalizedMessage)
        throw e
      }
    }
    val scheme: String = if (uri.getScheme == null) "http" else uri.getScheme
    val host: String = if (uri.getHost == null) "localhost" else uri.getHost
    var port: Int = uri.getPort
    if (port == -1) {
      if (scheme.equalsIgnoreCase("http")) {
        port = 80
      }
      else if (scheme.equalsIgnoreCase("https")) {
        port = 443
      }
    }
    if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
      System.err.println("Only HTTP(S) is supported.")
      return
    }
    val ssl: Boolean = scheme.equalsIgnoreCase("https")
    val b: Bootstrap = new Bootstrap
    try {
      //      b.group(new NioEventLoopGroup).channel(new NioSocketChannel).remoteAddress(host, port).handler(new ChannelInitializer[SocketChannel] {
      //        def initChannel(ch: SocketChannel) {
      //          val pipeline: ChannelPipeline = ch.pipeline
      //          val engine: SSLEngine = SSLContextFactory.getSSLSocket(sslConfiguration).createSSLEngine
      //          engine.setUseClientMode(true)
      //          if (ssl) pipeline.addLast("ssl", new SslHandler(engine))
      //          pipeline.addLast("codec", new HttpClientCodec)
      //          pipeline.addLast("chunked", new ChunkedWriteHandler)
      //          pipeline.addLast("handler", new HttpClientHandler(new FutureHTTPResponse))
      //        }
      //      })
      val ch: Channel = b.connect.sync.channel
      Thread.sleep(1000)
      val path: String = if (uri.getQuery != null) uri.getPath + "?" + uri.getQuery else uri.getPath
      val request: HttpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path)
      request.setHeader(HttpHeaders.Names.HOST, host)
      request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
      request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP)
      ch.write(request)
      ch.closeFuture.sync
    }
    finally {
      b.shutdown
    }
  }

  private var sslConfiguration: SSLConfiguration = null
  private var urlString: String = null
}

