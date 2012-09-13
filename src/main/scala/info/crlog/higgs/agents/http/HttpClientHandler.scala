package info.crlog.higgs.agents.http

import info.crlog.higgs.ClientHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.{HttpChunk, HttpResponse}
import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil

/**
 * Courtney Robinson <courtney@crlog.info>
 */
class HttpClientHandler(override val future: FutureHTTPResponse) extends ClientHandler[AnyRef](future) {
  private var readingChunks: Boolean = false

  override def messageReceived(ctx: ChannelHandlerContext, msg: AnyRef) {
    if (future.channel == None) {
      //if the future doesn't have a channel yet then assign this
      future.channel = Some(ctx.channel())
    }
    //if we're not reading a chunked response, which is the default
    if (!readingChunks) {
      val response: HttpResponse = msg.asInstanceOf[HttpResponse]
      //set response status and protocol version
      future.status = Some(response.getStatus)
      future.protocolVersion = Some(response.getProtocolVersion)
      //do we have any headers in the response?
      if (!response.getHeaderNames.isEmpty) {
        import scala.collection.JavaConversions._
        for (name <- response.getHeaderNames) {
          for (value <- response.getHeaders(name)) {
            future.headers += name -> value
          }
        }
      }
      if (response.getTransferEncoding.isMultiple) {
        //if response is chunked, don't use this code path next time
        readingChunks = true
        future.isChunked = true
      } else {
        //if it's not chunked, we should have all content
        val content: ByteBuf = response.getContent
        if (content.readable) {
          //set the response content
          future.content.append(content.toString(CharsetUtil.UTF_8))
          future.onMessage(ctx)
        }
      }
    } else {
      //if we are reading chunks
      val chunk: HttpChunk = msg.asInstanceOf[HttpChunk]
      if (chunk.isLast) {
        readingChunks = false
        future.onMessage(ctx)
      } else {
        future.content.append(chunk.getContent.toString(CharsetUtil.UTF_8))
      }
    }
  }
}

