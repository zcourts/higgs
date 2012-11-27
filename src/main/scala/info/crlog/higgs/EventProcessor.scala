package info.crlog.higgs

import io.netty.channel.{ChannelPipeline, Channel, ChannelHandlerContext}
import management.ManagementFactory
import javax.net.ssl.SSLEngine
import io.netty.handler.ssl.SslHandler
import ssl.{SSLConfiguration, SSLContextFactory}
import java.util.concurrent._
import org.slf4j.LoggerFactory
import scala.Some


/**
 * Courtney Robinson <courtney@crlog.info>
 */

trait EventProcessor[T, M, SerializedMsg] {
  val log = LoggerFactory.getLogger(getClass)
  var logAllExceptions = false
  val notificationListeners = new ConcurrentHashMap[Event.Value,
    ConcurrentLinkedQueue[(ChannelHandlerContext,
      Option[Throwable]) => Boolean]]()
  /**
   * A list of subscribed functions and a "validation" callback which determines on a per
   * message basis of the given callback "wants" the given message
   */
  val subscribers = new ConcurrentHashMap[T, ConcurrentLinkedQueue[
    ((T, M) => Boolean, (Channel, M) => Unit)]]()
  val messageQueue = new LinkedTransferQueue[(ChannelHandlerContext, SerializedMsg)]()
  val eventQueue = new LinkedTransferQueue[(Event.Value, ChannelHandlerContext, Option[Throwable])]()
  var processMessageQueue = true
  var processEventQueue = true
  val serializer: Serializer[M, SerializedMsg]
  val SSLclientMode: Boolean
  val availableProcessors = Runtime.getRuntime().availableProcessors()
  //http://www.informit.com/guides/content.aspx?g=dotnet&seqNum=588
  //yes, we're not doing .NET but its reasonable enough
  val maxThreads = availableProcessors * 25
  val threadPool: ExecutorService = Executors.newFixedThreadPool(maxThreads)

  /**
   * Add a function to be invoked when a supported event has occurred.
   * By default this function does not consume the event
   * The first parameter to the function, Event is the handler event type for e.g.
   * channel active/inactive or exception caught...
   * The second parameter is the handler context provided by Netty
   * The third parameter is an optional exception which is the cause of the notification
   * @param fn
   */
  def ++[U](e: Event.Value, fn: (ChannelHandlerContext, Option[Throwable]) => U) {
    var q = notificationListeners.get(e)
    if (q == null) {
      q = new ConcurrentLinkedQueue[(ChannelHandlerContext, Option[Throwable]) => Boolean]()
      notificationListeners.put(e, q)
    }
    q.add(
      ((ctx: ChannelHandlerContext, c: Option[Throwable]) => {
        fn(ctx, c)
        false
      })
    )
  }

  /**
   * Adds a function to be notified when the given event happens.
   * If the function returns tru it is assumed the function wants to consume the event
   * and prevent other subscribers (not already notified) of the same event receiving it.
   * @param e
   * @param fn
   */
  def on(e: Event.Value, fn: (ChannelHandlerContext, Option[Throwable]) => Boolean) {
    ++(e, fn)
  }

  //process event queue by default
  startProcessingEventQueue()

  def startProcessingEventQueue() {
    //emit event off of netty threads
    threadPool.submit(new Runnable {
      def run() {
        while (processEventQueue) {
          try {
            val bufferedEvent = eventQueue.take()
            val event = bufferedEvent._1
            val context = bufferedEvent._2
            val cause = bufferedEvent._3
            cause match {
              case None =>
              case Some(s) => {
                if (logAllExceptions)
                  log.error(s.getMessage, s)
              }
            }
            val fnList = notificationListeners.get(event)
            if (fnList != null) {
              var consumed = false
              val it = fnList.iterator()
              while (it.hasNext()) {
                val listener = it.next()
                if (!consumed) {
                  if (listener(context, cause)) {
                    consumed = true
                  }
                }
              }
            }
          } catch {
            case e => {
              log.warn("An event handler caused an unhandled exception", e)
            }
          }
        }
      }
    })

  }

  /**
   * Notify any notification listeners attached to this event processor
   * @param event the event this notification is for
   * @param context the handler context supplied by Netty for this event
   * @param cause if available, the cause of this notification
   */
  def emit(event: Event.Value, context: ChannelHandlerContext, cause: Option[Throwable]) {
    eventQueue.add((event, context, cause))
  }

  //start by default
  startProcessingMessaged()

  def startProcessingMessaged() {
    //thread to process messages queue
    threadPool.submit(new Runnable {
      def run() {
        while (processMessageQueue) {
          try {
            val bufferedMessage = messageQueue.take()
            val ctx = bufferedMessage._1
            val msg = bufferedMessage._2
            emit(Event.MESSAGE_RECEIVED, ctx, None)
            message(ctx, msg)
          } catch {
            case e => {
              log.warn("A message listener caused an uncaught exception", e)
            }
          }
        }
      }
    })
  }

  def emitMessage(ctx: ChannelHandlerContext, msg: SerializedMsg) {
    messageQueue.add((ctx, msg))
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
    //perform notifications off of netty threads
    threadPool.submit(new Runnable {
      def run() {
        val listeners = new java.util.ArrayList[((T, M) => Boolean, (Channel, M) => Unit)]()
        //get subscribers of "All" messages, i.e. subscribers to an empty string
        if (topic != allTopicsKey()) {
          //if topic is not already an empty string
          val q = subscribers.get(allTopicsKey())
          if (q != null) {
            //add if exists
            listeners.addAll(q)
          }
        }
        //get actual subscribers to this specific topic
        val q = subscribers.get(topic)
        if (q != null) {
          listeners.addAll(q)
        }
        //invoke each function
        val it = listeners.iterator()
        while (it.hasNext()) {
          val tuple = it.next()
          val wants = tuple._1(topic, message) //does this callback want to be invoked for this message?
          if (wants) {
            tuple._2(channel, message)
          }
        }
      }
    })
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
    var q = subscribers.get(topic)
    if (q == null) {
      q = new ConcurrentLinkedQueue[((T, M) => Boolean, (Channel, M) => Unit)]()
      subscribers.put(topic, q)
    }
    q.add(((wants, fn)))
  }

  def unsubscribe(topic: T) {
    subscribers.remove(topic)
  }

  /**
   * Checks if there is a subscription to the given topic already
   * @param topic
   * @return    true if there is, false otherwise
   */
  def listening(topic: T) = subscribers.contains(topic)

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
