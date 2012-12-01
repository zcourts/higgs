package info.crlog.higgs.protocols.http

import java.net.URL
import io.netty.handler.codec.http.{HttpHeaders, HttpVersion, HttpMethod}
import collection.mutable.{ListBuffer, Map}
import io.netty.handler.codec.http.multipart.{DiskAttribute, DiskFileUpload}

object HttpRequestBuilder {
  /**
   * Only a single instance of the requester.
   * EventProcessor uses a thread pool to process requests/responses
   * so its more efficient to only use one.
   */
  val requester = new RequestProcessor()
}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class HttpRequestBuilder {
  var requestURL = new URL("http://localhost/")
  var requestMethod: HttpMethod = HttpMethod.GET
  val urlParameters: Map[String, Any] = Map.empty[String, Any]
  /**
   * A key value pair of form fields  to send in a POST or PUT request.
   * Values can be strings (numbers get .toString() automatically) NOT FILES
   */
  val formParameters: Map[String, Any] = Map.empty[String, Any]
  /**
   * Set of files to upload
   */
  val formFiles: ListBuffer[HttpFile] = ListBuffer.empty[HttpFile]
  /**
   * Set of files to upload where 1 name/key has many files
   */
  val formMultiFiles: Map[String, List[PartialHttpFile]] = Map.empty[String, List[PartialHttpFile]]
  /**
   * Defaults to true. If set to false and files are provided in a PUT or POST
   * request then only file names will be sent.
   */
  var multiPart = true
  val requestHeaders: Map[String, String] = Map.empty[String, String]
  val requestCookies: Map[String, String] = Map.empty[String, String]
  var compressionEnabled: Boolean = false
  var shutdownAfter: Boolean = true
  var httpVersion: HttpVersion = HttpVersion.HTTP_1_1
  var addDefaultHeaders: Boolean = true
  var requestContentType: String = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
  var USER_AGENT: String = "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)"
  //default header values
  var header_connection_value = HttpHeaders.Values.CLOSE
  var header_accept_encoding = HttpHeaders.Values.GZIP + ',' + HttpHeaders.Values.DEFLATE
  var header_accept_charset = "ISO-8859-1,utf-8;q=0.7,*;q=0.7"
  var header_accept_lang = "en"
  //attributes used in generating the request or processing the response but aren't
  //exactly configurable as such
  var useSSL = false

  /**
   * To support continual method chaining this returns a new instance of
   * {@link HttpRequestBuilder} allowing you to construct and send request after request
   * without having to manually create a new instance.
   * NOTE: It does not copy any of the current settings from the currently referenced instance
   * it instead returns a completely new instance as in {@code new HttpRequestBuilder()}
   * @return
   */
  def clear(): HttpRequestBuilder = {
    new HttpRequestBuilder()
  }

  //default behaviour when dealing with Files
  DiskFileUpload.deleteOnExitTemporaryFile = true
  DiskFileUpload.baseDirectory = null
  DiskAttribute.deleteOnExitTemporaryFile = true
  DiskAttribute.baseDirectory = null

  def path(): String = {
    if (url().getPath().isEmpty) {
      "/"
    } else {
      url().getPath()
    }
  }

  /**
   * Set/Change the URL this request is being made to
   * @param url
   * @return
   */
  def url(url: URL): HttpRequestBuilder = {
    this.requestURL = url
    this
  }

  def url(): URL = requestURL

  /**
   * If true then add support for compressing/de-compressing HTTP content
   * @param b
   * @return
   */
  def compress(b: Boolean): HttpRequestBuilder = {
    this.compressionEnabled = b
    this
  }

  /**
   * If true (default) then once a response is received close the connection
   * @param b
   * @return
   */
  def shutdown(b: Boolean): HttpRequestBuilder = {
    this.shutdownAfter = b
    this
  }

  /**
   * HTTP Version, defaults to 1.1
   * @param v
   * @return
   */
  def version(v: HttpVersion): HttpRequestBuilder = {
    this.httpVersion = v
    this
  }

  /**
   * If true (default) then HOST,CONNECTION and USER_AGENT headers are added automatically.
   * @param b
   * @return
   */
  def defaultHeaders(b: Boolean): HttpRequestBuilder = {
    this.addDefaultHeaders = b
    this
  }

  /**
   * HTTP content type, default "text/plain"
   * @param t
   * @return
   */
  def contentType(t: String): HttpRequestBuilder = {
    this.requestContentType = t
    this
  }

  /**
   * Sets the client user agent. Default:
   * "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)"
   * @param a
   * @return
   */
  def userAgent(a: String): HttpRequestBuilder = {
    this.USER_AGENT = a
    this
  }

  /**
   * The method to use for this request, i.e. GET,POST,PUT,DELETE,HEAD etc
   * @param m
   * @return
   */
  def method(m: HttpMethod): HttpRequestBuilder = {
    this.requestMethod = m
    this
  }

  /**
   * Add a single header
   * @param name  header name
   * @param value  header value
   * @return
   */
  def header(name: String, value: String): HttpRequestBuilder = {
    this.requestHeaders += name -> value
    this
  }

  /**
   * Add all the name value pairs in the given map as headers
   * @param h
   * @return
   */
  def headers(h: Map[String, String]): HttpRequestBuilder = {
    this.requestHeaders ++= h
    this
  }


  /**
   * Add all the name value pairs in the given map as headers
   * @param h
   * @return
   */
  def headers(h: collection.immutable.Map[String, String]): HttpRequestBuilder = {
    this.requestHeaders ++= h
    this
  }

  /**
   * Add a single cookie
   * @param name  cookie name
   * @param value  cookie value
   * @return
   */
  def cookie(name: String, value: String): HttpRequestBuilder = {
    this.requestCookies += name -> value
    this
  }

  /**
   * Add all the name value pairs in the given map as cookies
   * @param h
   * @return
   */
  def cookies(h: Map[String, String]): HttpRequestBuilder = {
    this.requestCookies ++= h
    this
  }


  /**
   * Add all the name value pairs in the given map as cookies
   * @param h
   * @return
   */
  def cookies(h: collection.immutable.Map[String, String]): HttpRequestBuilder = {
    this.requestCookies ++= h
    this
  }

  /**
   * Add a single query string parameter
   * @param name  query string name/key
   * @param value  value accept Any, toString() is called to allow numeric values
   * @return
   */
  def query(name: String, value: Any): HttpRequestBuilder = {
    this.urlParameters += name -> value.toString
    this
  }

  /**
   * Add all the name value pairs in the given map query string parameters
   * Values will have toString() called
   * @param h
   * @return
   */
  def query(h: Map[String, Any]): HttpRequestBuilder = {
    this.urlParameters ++= h
    this
  }


  /**
   * Add all the name value pairs in the given map query string parameters
   * Values will have toString() called
   * @param h
   * @return
   */
  def query(h: collection.immutable.Map[String, Any]): HttpRequestBuilder = {
    this.urlParameters ++= h
    this
  }

  /**
   * Explicitly set multi-part.
   * The default is true, if b is set to false and files are added then
   * the files WILL NOT BE UPLOADED, only the file names will be sent in the
   * POST or PUT request. i.e. b must be true to upload the files themselves.
   * This is the default behaviour so only use if you intend to set to false
   * @param b
   * @return
   */
  def fileMultiPart(b: Boolean): HttpRequestBuilder = {
    this.multiPart = b
    this
  }

  /**
   * Add a single file parameter
   * {@link java.io.File} values will be handled properly
   * @param value  the file to upload
   * @return
   */
  def file(value: HttpFile): HttpRequestBuilder = {
    this.formFiles += value
    this
  }

  /**
   * Add a single file parameter which has multiple files associated with it
   * {@link java.io.File} values will be handled properly.
   * Name is the key that all the {@link PartialHttpFile}s will be uploaded under
   * @param name  string name/key
   * @param value  the file to upload
   * @return
   */
  def file(name: String, value: List[PartialHttpFile]): HttpRequestBuilder = {
    this.formMultiFiles += name -> value
    this
  }

  /**
   * Add a set of files to be uploaded
   * {@link java.io.File} values will be handled properly
   * @param h the files to be uploaded
   * @return
   */
  def file(h: List[HttpFile]): HttpRequestBuilder = {
    this.formFiles ++= h
    this
  }

  /**
   * Add a set of files to be uploaded
   * {@link java.io.File} values will be handled properly
   * @param h the files to be uploaded
   * @return
   */
  def file(h: ListBuffer[HttpFile]): HttpRequestBuilder = {
    this.formFiles ++= h
    this
  }

  def fileDeleteTempFilesOnExit(b: Boolean): HttpRequestBuilder = {
    DiskFileUpload.deleteOnExitTemporaryFile = b
    DiskAttribute.deleteOnExitTemporaryFile = b
    this
  }

  def fileBaseDirectory(baseDir: String): HttpRequestBuilder = {
    DiskFileUpload.baseDirectory = baseDir
    DiskAttribute.baseDirectory = baseDir
    this
  }

  /**
   * Add a single form parameter
   * {@link java.io.File} values will NOT be handled properly they'll have toString() called
   * use {@link #file(String,File)} and its related builders instead
   * @param name  string name/key
   * @param value  value accept Any
   * @return
   */
  def form(name: String, value: Any): HttpRequestBuilder = {
    this.formParameters += name -> value
    this
  }

  /**
   * Add a single form parameter
   * {@link java.io.File} values will NOT be handled properly they'll have toString() called
   * use {@link #file(String,File)} and its related builders instead
   * @param h key value pair
   * @return
   */
  def form(h: Map[String, Any]): HttpRequestBuilder = {
    this.formParameters ++= h
    this
  }


  /**
   * Add a single form parameter
   * {@link java.io.File} values will NOT be handled properly they'll have toString() called
   * use {@link #file(String,File)} and its related builders instead
   * @param h key value pair
   * @return
   */
  def form(h: collection.immutable.Map[String, Any]): HttpRequestBuilder = {
    this.formParameters ++= h
    this
  }

  /**
   * Turn this into an HTTP GET request
   */
  def GET(): HttpRequestBuilder = {
    requestMethod = HttpMethod.GET
    this
  }

  /**
   * Turn this into an HTTP POST request
   */
  def POST(): HttpRequestBuilder = {
    requestMethod = HttpMethod.POST
    this
  }

  /**
   * Turn this into an HTTP PUT request
   */
  def PUT(): HttpRequestBuilder = {
    requestMethod = HttpMethod.PUT
    this
  }

  /**
   * Turn this into an HTTP DELETE request
   */
  def DELETE(): HttpRequestBuilder = {
    requestMethod = HttpMethod.DELETE
    this
  }

  /**
   * Perform an HTTP request using the parameters configured in this builder.
   * @param callback A function that will be invoked accepting the HTTP response
   * @tparam U
   */
  def build[U](callback: (HTTPResponse) => U): HttpRequestBuilder = {
    requestMethod match {
      case HttpMethod.GET => HttpRequestBuilder.requester.getOrDelete(this, callback)
      case HttpMethod.DELETE => HttpRequestBuilder.requester.getOrDelete(this, callback)
      case HttpMethod.POST => HttpRequestBuilder.requester.postOrPut(this, callback)
      //      case HttpMethod.PUT => HttpRequestBuilder.requester.postOrPut(this, callback)
      case _ => {
        throw new UnsupportedOperationException("HTTP method \"%s\" not supported" format (requestMethod))
      }
    }
    this
  }
}
