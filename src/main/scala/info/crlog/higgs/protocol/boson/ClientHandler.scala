package info.crlog.higgs.protocol.boson

import org.jboss.netty.channel._
import com.codahale.logula.Logging
import collection.mutable.ListBuffer
import info.crlog.higgs.protocol.{Message, MessageListener}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class ClientHandler extends SimpleChannelUpstreamHandler with Logging {
  private val listeners = new ListBuffer[MessageListener]

  override def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent): Unit = {
    if (e.isInstanceOf[ChannelStateEvent]) {
    }
    super.handleUpstream(ctx, e)
  }

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
  }

  override def channelInterestChanged(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit = {
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
    //handle message
    log.debug("Message received" + e.getMessage)
    var msg: Message = null
    if (e.isInstanceOf[Message]) {
      msg = e.asInstanceOf[Message]
    }
    listeners foreach {
      listener => listener.onMessage(msg)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    e.getChannel.close
  }

  /**
   * Adds a listener that will receive messages
   */
  def addListener(listener: MessageListener) = {
    listeners += listener
  }
}