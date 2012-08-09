package info.crlog.higgs.agents.http

import info.crlog.higgs.FutureResponse
import io.netty.handler.codec.http.{HttpVersion, HttpResponseStatus}
import collection.mutable.ListBuffer
import io.netty.channel.ChannelHandlerContext

/**
 * Courtney Robinson <courtney@crlog.info>
 */

class FutureHTTPResponse extends FutureResponse {

  val content = new StringBuilder
  var isChunked: Boolean = false
  var headers = scala.collection.mutable.Map.empty[String, String]
  var protocolVersion: Option[HttpVersion] = None
  var status: Option[HttpResponseStatus] = None
  var isReady = false


  /**
   * Invoked by handlers once they've constructed the entire response.
   * This will cause all listeners to be notified
   */
  def onMessage(context: ChannelHandlerContext) {
    //prevent sending notification multiple times
    if (!isReady) {
      isReady = true
      onMessage(context, content.mkString)
    }
  }

}
