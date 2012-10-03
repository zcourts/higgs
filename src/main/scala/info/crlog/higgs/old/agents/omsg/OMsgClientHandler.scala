package info.crlog.higgs.agents.omsg

import java.util.logging.{Level, Logger}
import io.netty.channel.{ChannelInboundMessageHandlerAdapter, ChannelHandlerContext}
import collection.mutable
import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class OMsgClientHandler
(listeners: mutable.Map[Class[AnyRef], (Any) => Unit])
  extends ChannelInboundMessageHandlerAdapter[Array[Byte]] {

  val logger: Logger = Logger.getLogger(getClass.getName)
  val packer = new OMsgPacker
  private var ctx: ChannelHandlerContext = null

  override def channelActive(ctx: ChannelHandlerContext) {
    this.ctx = ctx
    println("context assigned")
  }

  def send[T <: Serializable](msg: T) = {
    isConnected
    if (!ctx.channel.isOpen) {
      throw new IllegalStateException("Connection to the server is no longer open")
    }
    val out = ctx.nextOutboundMessageBuffer
    out.add(packer.toBytes(msg))
    ctx.flush()
    this
  }

  def messageReceived(ctx: ChannelHandlerContext, msg: Array[Byte]) {
    val unpacked = packer.get(msg)
    val clazz = unpacked.asInstanceOf[AnyRef].getClass.asInstanceOf[Class[AnyRef]]
    val className = clazz.getName
    listeners.get(clazz) match {
      case None => logger.warning("Interaction of type %s received but no listeners have been registered for this type".format(className))
      case Some(clz) => {
        clz(unpacked)
      }
    }
  }

  val exceptionListeners = mutable.HashSet.empty[(Throwable) => Boolean]

  /**
   * Adds a function that is called when an exception occurs
   * If the function returns false it is assumed the function wants to
   * consume the exception. This means no other listener will be notified of the exception
   * @param l
   */
  def addExceptionListener(l: (Throwable) => Boolean) {
    exceptionListeners += l
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    if (exceptionListeners.isEmpty) {
      logger.log(Level.WARNING, "A:Unexpected exception from downstream.", cause)
    } else {
      var consumed = false
      for (listener <- exceptionListeners) {
        if (!consumed)
          if (!listener(cause)) {
            consumed = true
          }
      }
    }
  }

  protected def isConnected() {
    if (ctx == null) {
      throw new IllegalStateException("This client is not connected")
    }
  }
}

