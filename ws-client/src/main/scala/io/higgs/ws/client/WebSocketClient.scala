package io.higgs.ws.client

import java.net.URI

import scala.collection.mutable.ListBuffer

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
case class WebSocketClient(uri: URI, autoConnect: Boolean = true, autoReConnect: Boolean = true) {
  protected val onMsgL = ListBuffer.empty[(String) => _]
  protected val onBinL = ListBuffer.empty[(Array[Byte]) => _]
  protected val onPingL = ListBuffer.empty[() => _]
  protected val onCloseL = ListBuffer.empty[() => _]
  protected val onConnL = ListBuffer.empty[() => _]
  protected val onErrorL = ListBuffer.empty[(Throwable) => _]

  protected lazy val client = new WebSocketClientProcessor {
    override protected val autoReconnect: Boolean = WebSocketClient.this.autoReConnect

    override protected def uri: URI = WebSocketClient.this.uri
  }

  protected lazy val defaultSubscriber = new WebSocketEventListener {
    override def onConnect(): Unit = onConnL.foreach(_ ())

    override def onClose(): Unit = onCloseL.foreach(_ ())

    override def onMessage(msg: String): Unit = onMsgL.foreach(_ (msg))

    override def onBinary(data: Array[Byte]): Unit = onBinL.foreach(_ (data))

    override def onPing(): Unit = onPingL.foreach(_ ())

    override def onError(t: Throwable): Unit = onErrorL.foreach(_ (t))
  }
  subscribe(defaultSubscriber)
  if (autoConnect) {
    connect()
  }

  def connect(): Unit = client.connect(true)

  def subscribe(l: WebSocketEventListener) = client.subscribe(l)

  def onConnect(f: () => _) = onConnL += f

  def onClose(f: () => _) = onCloseL += f

  def onMessage(f: (String) => _) = onMsgL += f

  def onBinary(f: (Array[Byte]) => _) = onBinL += f

  def onError(f: (Throwable) => _) = onErrorL += f

  def onPing(f: () => _) = onPingL += f

  @throws[IllegalStateException]("If the client is not connected AND auto reconnect is disabled")
  def send(o: String) = client.send(o)

  @throws[IllegalStateException]("If the client is not connected AND auto reconnect is disabled")
  def send[A](o: A)(implicit cvt: (A) => String) = client.send(o)

  @throws[IllegalStateException]("If the client is not connected AND auto reconnect is disabled")
  def send(o: Array[Byte]) = client.send(o)

}
