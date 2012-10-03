package info.crlog.higgs

import io.netty.channel.{Channel, ChannelHandlerContext}
import collection.mutable.ListBuffer
import collection.mutable


/**
 * Courtney Robinson <courtney@crlog.info>
 */

trait EventProcessor[T, M, SerializedMsg] {
  val q = new mutable.Queue[(Event.Value, ChannelHandlerContext, Option[Throwable])]()
  val notificationListeners = ListBuffer.empty[
    (Event.Value,
      ChannelHandlerContext,
      Option[Throwable]) => Unit]
  val subscribers = mutable.Map.empty[T, ListBuffer[(Channel, M) => Unit]]
  val serializer: Serializer[M, SerializedMsg]

  /**
   * Add a function to be invoked when a supported event has occurred.
   * The first parameter to the function, Event is the handler event type for e.g.
   * channel active/inactive or exception caught...
   * The second parameter is the handler context provided by Netty
   * The third parameter is an optional exception which is the cause of the notification
   * @param fn
   */
  def ++(fn: (Event.Value,
    ChannelHandlerContext,
    Option[Throwable]) => Unit) {
    notificationListeners += fn
    //if this listener has been added after an event has already been triggered
    //and the event hasn't been sent to anyone then emit the listener
    if (!notificationListeners.isEmpty) {
      while (!q.isEmpty) {
        val e = q.dequeue()
        fn(e._1, e._2, e._3)
      }
    }
  }


  /**
   * Notify any notification listeners attached to this event processor
   * @param event the event this notification is for
   * @param context the handler context supplied by Netty for this event
   * @param cause if available, the cause of this notification
   */
  def emit(event: Event.Value, context: ChannelHandlerContext, cause: Option[Throwable]) {
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
  def message(context: ChannelHandlerContext, value: SerializedMsg)

  /**
   * The topic used to represent "all topics" for example if T was a String then
   * this method could return an empty string ("") and any function that subscribes to
   * all topics would be placed under this topic. In the case of T being Class[T]
   * then Class[AnyRef] could be used to represent all topics.
   * @return The most generic form of topics supported
   */
  def allTopicsKey(): T

  /**
   * Notify all subscribed functions to the given topic
   * @param channel the Channel on which the event occurred. This is used to "respond" if supported
   * @param topic
   * @param message
   */
  def notifySubscribers(channel: Channel, topic: T, message: M) {
    val listeners = ListBuffer.empty[(Channel, M) => Unit]
    //get subscribers of "All" messages, i.e. subscribers to an empty string
    if (topic != allTopicsKey()) {
      //if topic is not already an empty string
      subscribers get (allTopicsKey()) match {
        case None =>
        case Some(list) => listeners ++= list
      }
    }
    //get actual subscribers to this specific topic
    subscribers get (topic) match {
      case None =>
      case Some(list) => listeners ++= list
    }
    //invoke each function
    listeners foreach {
      case fn => fn(channel, message)
    }
  }

  /**
   * @see EventProcessor#listen((Channel,M))
   * @param fn
   */
  def ++(fn: (Channel, M) => Unit) = listen(fn)

  /**
   * Subscribe a function to receive ALL messages this event processor receives
   * @param fn
   */
  def listen(fn: (Channel, M) => Unit) {
    listen(allTopicsKey(), fn)
  }

  /**
   * Listen for messages of the given topic
   * @param topic
   * @param fn  your callback to be invoked when a message with the topic is received
   */
  def listen(topic: T, fn: (Channel, M) => Unit) {
    subscribers
      .getOrElseUpdate(topic, ListBuffer.empty) += fn
  }

  /**
   * Given a message this method appropriately serializes and sends another message
   * back to the channel it was received from.
   * @param c
   * @param obj
   */
  def respond(c: Channel, obj: M) {
    c.write(serializer.serialize(obj))
  }

  /**
   * Convert a message to its serialized form. This uses serializer.serialize
   * @param obj
   * @return
   */
  def serialize(obj: M) = serializer.serialize(obj)
}
