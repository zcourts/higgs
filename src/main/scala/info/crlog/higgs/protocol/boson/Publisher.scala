package info.crlog.higgs.protocol.boson

import org.jboss.netty.channel._
import info.crlog.higgs.protocol.{HiggsPublisher, Message}

/**
 * publishers don't handle incoming messages, implementation is just for future extensions
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class Publisher extends HiggsPublisher {

  override def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    super.handleUpstream(ctx, e)
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
  }

  override def channelInterestChanged(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    e.getChannel.close
  }
}