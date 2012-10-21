package info.crlog.higgs.protocols.http

import java.net.URL
import io.netty.handler.codec.http.{HttpMethod, HttpVersion}

/**
 * Courtney Robinson <courtney@crlog.info>
 */

class HttpClient(var USER_AGENT: String = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)") {

  def DELETE(url: URL, listener: (HTTPResponse) => Unit,
             headers: Map[String, String] = Map.empty[String, String],
             cookies: Map[String, String] = Map.empty[String, String],
             block: Boolean = false,
             httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
             addDefaultHeaders: Boolean = true,
             shutdown: Boolean = true,
             contentType: String = "text/plain",
             gzip: Boolean = false) = {
    new HttpRequest(url, HttpMethod.DELETE, headers, cookies, block, httpVersion, addDefaultHeaders,
      shutdown, contentType = contentType, compressionEnabled = gzip).send(listener)
  }

  def GET(url: URL, listener: (HTTPResponse) => Unit,
          headers: Map[String, String] = Map.empty[String, String],
          cookies: Map[String, String] = Map.empty[String, String],
          block: Boolean = false,
          httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
          addDefaultHeaders: Boolean = true,
          shutdown: Boolean = true,
          contentType: String = "text/plain",
          gzip: Boolean = false) = {
    new HttpRequest(url, HttpMethod.GET, headers, cookies, block, httpVersion, addDefaultHeaders,
      shutdown, contentType = contentType, compressionEnabled = gzip).send(listener)
  }

  def POST(url: URL, listener: (HTTPResponse) => Unit,
           data: Map[String, Any] = Map.empty[String, Any],
           headers: Map[String, String] = Map.empty[String, String],
           cookies: Map[String, String] = Map.empty[String, String],
           block: Boolean = false,
           httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
           addDefaultHeaders: Boolean = true,
           shutdown: Boolean = true,
           contentType: String = "application/x-www-form-urlencoded",
           gzip: Boolean = false
            ) = {
    new HttpRequest(url, HttpMethod.POST, headers, cookies, block, httpVersion, addDefaultHeaders,
      shutdown, contentType = contentType, compressionEnabled = gzip, data = data).send(listener)
  }

  def PUT(url: URL, listener: (HTTPResponse) => Unit,
          data: Map[String, Any] = Map.empty[String, Any],
          headers: Map[String, String] = Map.empty[String, String],
          cookies: Map[String, String] = Map.empty[String, String],
          block: Boolean = false,
          httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
          addDefaultHeaders: Boolean = true,
          shutdown: Boolean = true,
          contentType: String = "application/x-www-form-urlencoded",
          gzip: Boolean = false
           ) = {
    new HttpRequest(url, HttpMethod.PUT, headers, cookies, block, httpVersion, addDefaultHeaders,
      shutdown, contentType = contentType, compressionEnabled = gzip, data = data).send(listener)
  }
}
