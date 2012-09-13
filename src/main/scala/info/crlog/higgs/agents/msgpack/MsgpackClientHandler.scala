package info.crlog.higgs.agents.msgpack

import java.util.logging.{Level, Logger}
import io.netty.channel.{ChannelInboundMessageHandlerAdapter, ChannelHandlerContext}
import collection.mutable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class MsgpackClientHandler[T <: Interaction](listeners: mutable.Map[Class[Any], (Any) => Unit])
  extends ChannelInboundMessageHandlerAdapter[Array[Byte]] {

  val logger: Logger = Logger.getLogger(getClass.getName)
  val packer = new Packing
  private var ctx: ChannelHandlerContext = null

  override def channelActive(ctx: ChannelHandlerContext) {
    this.ctx = ctx
    println("context assigned")
  }

  def send[T <: Interaction](msg: T) = {
    isConnected
    if (!ctx.channel.isOpen) {
      throw new IllegalStateException("Connection to the server is no longer open")
    }
    val out = ctx.nextOutboundMessageBuffer
    out.add(packer.packBytes(msg))
    ctx.flush()
    this
  }

  def messageReceived(ctx: ChannelHandlerContext, msg: Array[Byte]) {
    val unpacked = packer.unpackBytes(msg)
    val className = unpacked._1
    val clazz: Class[Any] = unpacked._2
    listeners.get(clazz) match {
      case None => logger.warning("Interaction of type %s received but no listeners have been registered for this type".format(className))
      case Some(clz) => {
        unpacked._3.asInstanceOf[Interaction].context = ctx
        clz(unpacked._3)
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    logger.log(Level.WARNING, "Unexpected exception from downstream.", cause)
    ctx.close
  }

  protected def isConnected() {
    if (ctx == null) {
      throw new IllegalStateException("This client is not connected")
    }
  }
}

