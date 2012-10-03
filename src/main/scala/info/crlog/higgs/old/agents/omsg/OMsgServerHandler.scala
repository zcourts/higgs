package info.crlog.higgs.agents.omsg

import collection.mutable
import collection.mutable.ListBuffer
import commands.{Command, Subscribe}
import io.netty.channel.{ChannelInboundMessageHandlerAdapter, ChannelHandlerContext}
import io.netty.logging.{InternalLoggerFactory, InternalLogger}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class OMsgServerHandler(
                         clients: mutable.Map[Class[Any], ListBuffer[ChannelHandlerContext]],
                         listeners: mutable.Map[Class[AnyRef], (Any) => Unit]
                         ) extends ChannelInboundMessageHandlerAdapter[Array[Byte]] {
  private val log: InternalLogger = InternalLoggerFactory.getInstance(getClass)
  val packer = new OMsgPacker

  def messageReceived(ctx: ChannelHandlerContext, data: Array[Byte]) {
    val unpacked = packer.get(data)
    val clazz = unpacked.asInstanceOf[AnyRef].getClass.asInstanceOf[Class[AnyRef]]
    val className = clazz.getName

    if (classOf[Command].isAssignableFrom(clazz)) {
      processCommand(className, clazz.asInstanceOf[Class[Any]], unpacked, ctx)
    } else {
      //get all functions that accept this class
      listeners.get(clazz) match {
        case None => log.warn("Interaction of type %s received but no listeners have been registered for this type".format(className))
        case Some(clz) => {
          clz(unpacked)
        }
      }
    }
  }


  /**
   * Processes a command. The command may or may not be forwarded to a subscribed function
   * For e.g.. the "Subscribe" command is consumed
   * @param className
   * @param command
   * @param msg
   * @param ctx
   * @return
   */
  protected def processCommand(className: String, command: Class[Any], msg: Any, ctx: ChannelHandlerContext): Any = {
    log.info("Processing command %s".format(className))
    if (classOf[Subscribe].equals(command)) {
      //subscribe this channel to classes of the given type
      clients.
        getOrElseUpdate(
        Class.forName(msg.asInstanceOf[Subscribe].clazz)
          .asInstanceOf[Class[Any]],
        ListBuffer.empty[ChannelHandlerContext]) += ctx
      log.info("Added subscriber for %s".format(msg.asInstanceOf[Subscribe].clazz))
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace
    ctx.close
  }
}
