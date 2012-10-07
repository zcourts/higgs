package info.crlog.higgs

import io.netty.channel.{ChannelPipeline, Channel, ChannelHandlerContext}
import collection.mutable.ListBuffer
import collection.mutable
import management.ManagementFactory
import javax.net.ssl.SSLEngine
import io.netty.handler.ssl.SslHandler
import ssl.{SSLConfiguration, SSLContextFactory}


/**
 * Courtney Robinson <courtney@crlog.info>
 */

trait EventProcessor[T, M, SerializedMsg] {
  val notificationListeners = mutable.Map.empty[Event.Value, ListBuffer[
    (ChannelHandlerContext,
      Option[Throwable]) => Unit]]
  /**
   * A list of subscribed functions and a "validation" callback which determines on a per
   * message basis of the given callback "wants" the given message
   */
  val subscribers = mutable.Map.empty[T, ListBuffer[((T, M) => Boolean, (Channel, M) => Unit)]]
  val serializer: Serializer[M, SerializedMsg]
  val SSLclientMode: Boolean

  /**
   * Add a function to be invoked when a supported event has occurred.
   * The first parameter to the function, Event is the handler event type for e.g.
   * channel active/inactive or exception caught...
   * The second parameter is the handler context provided by Netty
   * The third parameter is an optional exception which is the cause of the notification
   * @param fn
   */
  def ++(e: Event.Value, fn: (ChannelHandlerContext, Option[Throwable]) => Unit) {
    notificationListeners.getOrElseUpdate(e, ListBuffer.empty) += fn
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
    notificationListeners.get(event) match {
      case None => //TODO log event but no listeners
      case Some(fnList) => fnList foreach {
        case fn => fn(context, cause)
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
    val listeners = ListBuffer.empty[((T, M) => Boolean, (Channel, M) => Unit)]
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
      case tuple => {
        val wants = tuple._1(topic, message) //does this callback want to be invoked for this message?
        if (wants) {
          tuple._2(channel, message)
        }
      }
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
    listen(topic, fn, (t: T, m: M) => {
      true //want all messages by default, regardless of the message
    })
  }

  def listen(topic: T, fn: (Channel, M) => Unit, wants: (T, M) => Boolean) {
    subscribers
      .getOrElseUpdate(topic, ListBuffer.empty) += ((wants, fn))
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

  /**
   * Adds an SSL Handler to the channel pipeline
   * @param pipeline
   */
  def ssl(pipeline: ChannelPipeline) {
    val sslConfiguration: SSLConfiguration = new SSLConfiguration

    import scala.collection.JavaConversions._
    val arg = (for (arg <- ManagementFactory.getRuntimeMXBean().getInputArguments) yield {
      val parts = arg.split('=')
      if (parts.length >= 2)
        parts(0).substring(2) -> parts(1)
      else
        "" -> ""
    }).toMap
    arg.get("javax.net.ssl.keyStore") match {
      case None =>
      case Some(ksPath) => sslConfiguration.setKeyStorePath(ksPath)
    }
    arg.get("javax.net.ssl.keyStorePassword") match {
      case None =>
      case Some(ksPass) => sslConfiguration.setKeyStorePassword(ksPass)
    }
    arg.get("javax.net.ssl.trustStrore") match {
      case None =>
      case Some(tsPath) => sslConfiguration.setTrustStorePath(tsPath)
    }
    arg.get("javax.net.ssl.trustStorePassword") match {
      case None =>
      case Some(tsPass) => sslConfiguration.setTrustStorePassword(tsPass)
    }
    arg.get("javax.net.ssl.keyPassword") match {
      case None =>
      case Some(tsPass) => sslConfiguration.setKeyPassword(tsPass)
    }
    val engine: SSLEngine = SSLContextFactory.getSSLSocket(sslConfiguration).createSSLEngine
    engine.setUseClientMode(SSLclientMode)
    pipeline.addLast("ssl", new SslHandler(engine))
  }
}
