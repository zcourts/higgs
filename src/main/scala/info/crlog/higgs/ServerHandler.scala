package info.crlog.higgs

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundMessageHandlerAdapter
import io.netty.logging.InternalLogger
import io.netty.logging.InternalLoggerFactory


class ServerHandler[T]() extends ChannelInboundMessageHandlerAdapter[T] {
  val logger: InternalLogger = InternalLoggerFactory.getInstance(getClass)

  def messageReceived(ctx: ChannelHandlerContext, msg: T) {

  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    cause.printStackTrace
    ctx.close
  }
}


