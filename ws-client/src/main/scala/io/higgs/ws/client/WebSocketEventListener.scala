package io.higgs.ws.client

import org.slf4j.LoggerFactory

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
trait WebSocketEventListener {
  def onClose()

  def onBinary(data: Array[Byte])

  lazy val log = LoggerFactory.getLogger(getClass)

  def onConnect() = log.info("Connected")

  def onDisconnect() = log.info("Disconnected")

  def onPing() = log.debug("Ping")

  def onError(t: Throwable) = log.error("An error has occurred", t)

  def onMessage(msg: String)
}
