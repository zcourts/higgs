package info.crlog.higgs

import io.netty.channel.{ChannelStateHandler, ChannelHandlerContext}
import Event._

/**
 * Provide an implementation for both server and client handler.
 * Implementations are exptected to override and provide an events instance
 * @author Courtney Robinson <courtney@crlog.info>
 */
trait HiggsHandler[Topic, Msg, SerializedMsg] extends ChannelStateHandler {
  val events: EventProcessor[Topic, Msg, SerializedMsg]

  override def channelActive(ctx: ChannelHandlerContext) {
    events.emit(CHANNEL_ACTIVE, ctx, None)
  }

  override def channelRegistered(ctx: ChannelHandlerContext) {
    events.emit(CHANNEL_REGISTERED, ctx, None)
  }

  override def channelUnregistered(ctx: ChannelHandlerContext) {
    events.emit(CHANNEL_UNREGISTERED, ctx, None)
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    events.emit(CHANNEL_INACTIVE, ctx, None)
  }

  def messageReceived(ctx: ChannelHandlerContext, msg: SerializedMsg) {
    events.message(ctx, msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    events.emit(EXCEPTION_CAUGHT, ctx, Some(cause))
  }

}
