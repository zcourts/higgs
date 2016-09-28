package io.higgs.ws.client

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */

import io.netty.channel.{Channel, ChannelHandlerContext, ChannelPromise, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx._
import org.slf4j.LoggerFactory

class WebSocketClientHandler(handShaker: WebSocketClientHandshaker,
                             connector: WebSocketConnector,
                             processor: WebSocketConnectorEventProcessor) extends SimpleChannelInboundHandler[Any] {
  val log = LoggerFactory.getLogger(getClass)
  private var handshakeFuture: ChannelPromise = null

  def sync = handshakeFuture.sync()

  override def handlerAdded(ctx: ChannelHandlerContext) {
    handshakeFuture = ctx.newPromise
  }

  override def channelActive(ctx: ChannelHandlerContext) {
    handShaker.handshake(ctx.channel)
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    log.info("WebSocket Client disconnected!")
  }

  def channelRead0(ctx: ChannelHandlerContext, msg: Any) {
    val ch: Channel = ctx.channel
    if (!handShaker.isHandshakeComplete) {
      handShaker.finishHandshake(ch, msg.asInstanceOf[FullHttpResponse])
      log.info("WebSocket Client connected!")
      handshakeFuture.setSuccess()
      processor.onConnect()
      return
    }
    msg match {
      case response: FullHttpResponse =>
        processor.invalidResponse(response)
      case _ =>
        msg.asInstanceOf[WebSocketFrame] match {
          case textFrame: TextWebSocketFrame =>
            log.debug("WebSocket Client received message: " + textFrame.text)
            processor.onMessage(textFrame.text())
          case binary: BinaryWebSocketFrame =>
            log.debug("WebSocket Client received message")
            val obj = binary.content()
            val data = new Array[Byte](obj.readableBytes())
            obj.readBytes(data)
            processor.onBinary(data)
          case _: PingWebSocketFrame =>
            log.debug("WebSocket Client received ping")
            processor.onPing()
          case _: CloseWebSocketFrame =>
            log.info("WebSocket server requested we close the connection")
            if (!processor.onClose) {
              connector.disconnect() //use disconnect to prevent auto reconnect
            } else {
              ch.close //close the channel directly to trigger auto reconnect
            }
          case _ => log.warn(s"Unknown/Unsupported message type received $msg")
        }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    processor.onError(cause)
    if (!handshakeFuture.isDone) {
      handshakeFuture.setFailure(cause)
    }
    ctx.close
  }
}
