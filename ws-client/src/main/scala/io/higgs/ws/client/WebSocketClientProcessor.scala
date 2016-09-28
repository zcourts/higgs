package io.higgs.ws.client

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import io.higgs.core.EventLoopGroups
import io.netty.buffer.Unpooled
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.websocketx.{BinaryWebSocketFrame, TextWebSocketFrame}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
trait WebSocketClientProcessor extends WebSocketConnector with WebSocketConnectorEventProcessor {

  protected case class Data(text: Option[String], binary: Option[Array[Byte]])

  protected def hw: Int = 100

  protected val autoReconnect: Boolean
  protected lazy val listeners = ListBuffer.empty[WebSocketEventListener]
  protected lazy val q = new ArrayBlockingQueue[Data](hw, true)
  protected lazy val ref = new AtomicReference[Boolean]()
  override lazy val channelClass = classOf[NioSocketChannel]
  override lazy val group: EventLoopGroup = EventLoopGroups.nio

  protected def trigger(o: Data): Unit = {
    if (disconnected.get()) {
      throw new IllegalStateException("Client not connected")
    }
    ensureProcessor
    val start = System.nanoTime()
    log.debug("Queuing message to be sent")
    q.put(o)
    log.debug(s"Message queued, took ${(System.nanoTime() - start) nanos}")
  }

  protected def defaultConsumerThread = () => {
    group.submit(new Runnable {
      override def run(): Unit = {
        try {
          val start = System.nanoTime()
          log.debug(s"Sending ${q.size()} queued messages")
          defaultConsumer()
          log.debug(s"All queued messages sent ${(System.nanoTime() - start) nanos}")
        } finally {
          ref.set(false) //always reset reference to false when consumer dies
        }
      }
    })
    true
  }

  protected def defaultConsumer(): Unit = {
    val ch = channel.get()
    if (ch != null && ch.isWritable) {
      try {
        var item: Data = null
        while ( {
          item = q.poll(10, TimeUnit.SECONDS)
          item != null
        }) {
          item.text.map(m => new TextWebSocketFrame(m)).map(ch.write)
          item.binary.map(m => new BinaryWebSocketFrame(Unpooled.wrappedBuffer(m))).map(ch.write)
        }
      } finally {
        ch.flush()
      }
    } else if (disconnected.get() && (ch == null | (ch != null && !ch.isWritable))) {
      if (!q.isEmpty) {
        log.warn(s"Client is disconnected with ${q.size()} unsent messages and will not auto reconnect")
      }
    }
  }

  protected def ensureProcessor = ref.compareAndSet(false, defaultConsumerThread())

  override def onConnect(): Unit = {
    ensureProcessor
    listeners.foreach(_ onConnect())
  }

  override def onPing(): Unit = listeners.foreach(_ onPing())

  override def onClose: Boolean = {
    listeners.foreach(_ onClose())
    autoReconnect
  }

  override def onBinary(data: Array[Byte]): Unit = listeners.foreach(_ onBinary data)

  override def onMessage(msg: String): Unit = listeners.foreach(_ onMessage msg)

  override def onError(e: Throwable): Unit = listeners.foreach(_ onError e)

  def connect(): Unit = connect(true)

  def connect(autoReconnect: Boolean): Unit = connect(if (autoReconnect) 1 else -1)

  @throws[IllegalStateException]("If the client is not connected AND auto reconnect is disabled")
  def send(o: String) = trigger(Data(Option(o), None))

  @throws[IllegalStateException]("If the client is not connected AND auto reconnect is disabled")
  def send[A](o: A)(implicit cvt: (A) => String) = trigger(Data(Option(cvt(o)), None))

  @throws[IllegalStateException]("If the client is not connected AND auto reconnect is disabled")
  def send(o: Array[Byte]) = trigger(Data(None, Option(o)))

  def subscribe(l: WebSocketEventListener) = listeners += l
}
