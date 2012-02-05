package info.crlog.higgs.protocol

import org.jboss.netty.channel.{ChannelStateEvent, ChannelHandlerContext}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 01/02/12
 */

trait MessageListener {
  def onMessage(msg: Message)
  def onConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent): Unit ={

  }
}