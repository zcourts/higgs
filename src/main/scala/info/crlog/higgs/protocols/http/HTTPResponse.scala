package info.crlog.higgs.protocols.http

import collection.mutable
import collection.mutable.ListBuffer
import io.netty.handler.codec.http.{HttpTransferEncoding, HttpVersion, HttpResponseStatus}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class HTTPResponse {
  var transferEncoding: HttpTransferEncoding = null
  var protocolVersion: HttpVersion = null
  var status: HttpResponseStatus = null
  val data = new StringBuilder()
  val headers = mutable.Map.empty[String, ListBuffer[String]]

  override def toString() = "%s\n %s\n %s\n %s\n %s\n" format(status, transferEncoding, protocolVersion, headers, data.mkString)
}
