package info.crlog.higgs

import io.netty.channel.{Channel, ChannelHandlerContext}
import info.crlog.higgs.FutureResponse.Event

/**
 * Courtney Robinson <courtney@crlog.info>
 */

trait EventListener [T]{
  /**
   * Add a function to be invoked when a supported event has occurred.
   * The first parameter to the function, Event is the handler event type for e.g.
   * channel active/inactive or exception caught...
   * The second parameter is the handler context provided by Netty
   * The third parameter is an optional exception which is the cause of the notification
   * @param event the event that has occurred
   * @param ctx  Channel handler context
   * @param ex  the exception, IF ANY that caused the event
   */
  def onEvent(event: Event, ctx: ChannelHandlerContext, ex: Option[Throwable])

  /**
   * Add a function to be invoked when a message is received
   *
   * @param channel  the channel on which the message was received
   * @param msg the message object
   */
  def onMessage(channel: Channel, msg: T)
}
