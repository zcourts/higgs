package info.crlog.higgs.agents.http

import info.crlog.higgs.EventListener
import info.crlog.higgs.EventProcessor._
import io.netty.channel.ChannelHandlerContext

/**
 * Simple HTTP event listener class.
 * Do not share event listeners among HTTP requests
 * Courtney Robinson <courtney@crlog.info>
 */

trait HTTPEventListener extends EventListener[String] {
  val response = new EventProcessor
  response ++ this

  def onEvent(event: Event, ctx: ChannelHandlerContext, ex: Option[Throwable]) {
    //NO OP
  }
}
