package info.crlog.higgs

import io.netty.channel.{ChannelHandlerContext, ChannelInboundMessageHandlerAdapter}
import io.netty.logging.{InternalLoggerFactory, InternalLogger}

case class ClientHandler[T](future: FutureResponse) extends ChannelInboundMessageHandlerAdapter[T] {

  val logger: InternalLogger = InternalLoggerFactory.getInstance(getClass)

  override def channelActive(ctx: ChannelHandlerContext) {
    notify(FutureResponse.CHANNEL_ACTIVE, ctx, None)
    super.channelActive(ctx)
  }

  override def channelRegistered(ctx: ChannelHandlerContext) {
    notify(FutureResponse.CHANNEL_REGISTERED, ctx, None)
    super.channelRegistered(ctx)
  }

  override def channelUnregistered(ctx: ChannelHandlerContext) {
    notify(FutureResponse.CHANNEL_UNREGISTERED, ctx, None)
    super.channelUnregistered(ctx)
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    notify(FutureResponse.CHANNEL_INACTIVE, ctx, None)
    super.channelInactive(ctx)
  }

  def messageReceived(ctx: ChannelHandlerContext, msg: T) {
    future.onMessage(ctx, msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    notify(FutureResponse.EXCEPTION_CAUGHT, ctx, Some(cause))
  }

  def notify(t: FutureResponse.Event, ctx: ChannelHandlerContext, cause: Option[Throwable]) {
    future.notify(t, ctx, cause)
  }
}

