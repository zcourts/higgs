package info.crlog.higgs.protocol.boson

import org.jboss.netty.channel._

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class ServerHandler extends SimpleChannelUpstreamHandler {

  override def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent): Unit = {
    super.handleUpstream(ctx, e)
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    //a client has sent a message
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    e.getChannel.close
  }
}