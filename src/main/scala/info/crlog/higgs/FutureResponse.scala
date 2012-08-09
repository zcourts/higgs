package info.crlog.higgs

import io.netty.channel.{ChannelHandlerContext, Channel}
import collection.mutable.ListBuffer
import collection.mutable

object FutureResponse {
  /**
   * Any event which this handler provides notifications for
   */
  type Event = String
  val CHANNEL_ACTIVE = new Event("channel_active")
  val CHANNEL_INACTIVE = new Event("channel_inactive")
  val CHANNEL_REGISTERED = new Event("channel_registered")
  val CHANNEL_UNREGISTERED = new Event("channel_unregistered")
  val EXCEPTION_CAUGHT = new Event("exception_caught")
}

/**
 * Courtney Robinson <courtney@crlog.info>
 */

abstract class FutureResponse {
  var channel: Option[Channel] = None
  val q = new mutable.Queue[(FutureResponse.Event, ChannelHandlerContext, Option[Throwable])]()
  val msgQ = new mutable.Queue[(ChannelHandlerContext, AnyRef)]()

  val msgListeners = ListBuffer.empty[(Channel, Any) => Unit]
  val notificationListeners = ListBuffer.empty[
    (FutureResponse.Event,
      ChannelHandlerContext,
      Option[Throwable]) => Unit]

  /**
   * Add a function to be invoked when a message is received
   * @param fn
   */
  def ++(fn: (Channel, Any) => Unit) {
    msgListeners += fn
    while (!msgQ.isEmpty) {
      val msg = msgQ.dequeue()
      fn(msg._1.channel(), msg._2)
    }
  }

  /**
   * Add a function to be invoked when a supported event has occurred.
   * The first parameter to the function, Event is the handler event type for e.g.
   * channel active/inactive or exception caught...
   * The second parameter is the handler context provided by Netty
   * The third parameter is an optional exception which is the cause of the notification
   * @param fn
   */
  def ++(fn: (FutureResponse.Event,
    ChannelHandlerContext,
    Option[Throwable]) => Unit) {
    notificationListeners += fn
    //if this listener has been added after an event has already been triggered
    //and the event hasn't been sent to anyone then notify the listener
    if (!notificationListeners.isEmpty) {
      while (!q.isEmpty) {
        val e = q.dequeue()
        fn(e._1, e._2, e._3)
      }
    }
  }

  /**
   * Adds an event listener to be notifed on events and messages
   * NOTE: This just proxies the functional implementations of
   * {@code FutureResponse#++} and wrap the event listener object.
   * It makes working with the response object easier/cleaner from Java
   * @param eventListener
   */
  def ++[T](eventListener: EventListener[T]) {
    //delegate both on event and message

    ++((e: FutureResponse.Event,
        ctx: ChannelHandlerContext,
        ex: Option[Throwable]) => {
      eventListener.onEvent(e, ctx, ex)
    })
    ++((ch: Channel, msg: Any) => {
      eventListener.onMessage(ch, msg.asInstanceOf[T])
    })
  }

  /**
   * Notify any notification listeners attached to this future
   * @param event the event this notification is for
   * @param context the handler context supplied by Netty for this event
   * @param cause if available, the cause of this notification
   */
  def notify(event: FutureResponse.Event, context: ChannelHandlerContext, cause: Option[Throwable]) {
    cause match {
      case None =>
      case Some(s) => {
        s.printStackTrace()
      }
    }
    if (notificationListeners.isEmpty) {
      q.enqueue((event, context, cause))
    } else {
      notificationListeners map {
        case fn => fn(event, context, cause)
      }
    }
  }

  /**
   * Lets all attached message listeners know that a new message has been received
   * @param context the handler context provided by Netty
   * @param value the message that has been received
   */
  def onMessage[T](context: ChannelHandlerContext, value: T) {
    if (msgListeners.isEmpty) {
      msgQ.enqueue((context, value.asInstanceOf[AnyRef]))
    } else {
      msgListeners map {
        case fn => fn(context.channel(), value)
      }
    }
  }

}
