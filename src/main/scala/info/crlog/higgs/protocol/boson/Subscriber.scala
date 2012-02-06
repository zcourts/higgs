package info.crlog.higgs.protocol.boson

import org.jboss.netty.channel._
import info.crlog.higgs.protocol.{MessageListener, HiggsSubscriber}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class Subscriber(listener: MessageListener) extends HiggsSubscriber(listener) {
  override def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent): Unit = {
    super.handleUpstream(ctx, e)
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    var msg: Option[BosonMessage] = None
    if (e.getMessage.isInstanceOf[BosonMessage]) {
      msg = Some(e.getMessage.asInstanceOf[BosonMessage])
      listener.onMessage(msg.get)
    } else {
      listener.onMessage(new BosonMessage(e.getMessage))
      println("Invalid message, MSG:" + e.getMessage)
    }
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    e.getChannel.close
  }
}