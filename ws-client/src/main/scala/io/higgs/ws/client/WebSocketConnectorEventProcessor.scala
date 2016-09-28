package io.higgs.ws.client

import io.netty.handler.codec.http.FullHttpResponse
import io.netty.util.CharsetUtil
import org.slf4j.LoggerFactory

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
trait WebSocketConnectorEventProcessor {


  val log = LoggerFactory.getLogger(getClass)

  /**
    * Called if the server returned a normal HTTP response
    *
    * @param response the response the server returned
    */
  def invalidResponse(response: FullHttpResponse): Unit = {
    log.warn(s"Unexpected FullHttpResponse (getStatus=${response.getStatus}, content=${response.content.toString(CharsetUtil.UTF_8)})")
  }

  def onMessage(msg: String): Unit

  def onBinary(data: Array[Byte]): Unit = {
    log.warn("Binary message received, forcibly converted to string, you should override " +
      "WebSocketConnectorEventProcessor.onBinary to handle these messages properly")
    onMessage(new String(data))
  }

  def onPing(): Unit = {
    log.debug("Ping received")
  }

  def onConnect() = log.info("WebSocket client connected")

  /**
    * The server requested that we close the connection
    *
    * @return true if the client should automatically reconnect, false otherwise
    */
  def onClose: Boolean = true

  def onError(e: Throwable) = log.warn("An uncaught error has occurred", e)
}
