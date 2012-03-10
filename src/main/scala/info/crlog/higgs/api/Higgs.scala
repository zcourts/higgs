package info.crlog.higgs.api

import reflect.BeanProperty
import collection.mutable.{ListBuffer, HashMap}
import info.crlog.higgs.protocol._
import boson._

/**
 * Deprecated, not thread safe! Deadlock often occurs preventing messages being sent or received
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */
@Deprecated
class Higgs(var socketType: HiggsConstants.Value) {
  type ListenersList = ListBuffer[Function1[Message, Unit]]
  /**
   * The decoder Higgs uses to decode messages
   */
  @BeanProperty //default protocol decoder
  var decoder: Class[_ <: HiggsDecoder] = classOf[BosonDecoder]
  @BeanProperty //default protocol encoder
  var encoder: Class[_ <: HiggsEncoder] = classOf[BosonEncoder]
  @BeanProperty //default client request handler
  var clientHandler: Class[_ <: HiggsPublisher] = classOf[Publisher]
  var message: Class[_ <: Message] = classOf[BosonMessage]
  @BeanProperty //default server request handler
  var serverHandler: Class[_ <: HiggsSubscriber] = classOf[Subscriber]
  protected var publisher: Option[HiggsClient] = None
  protected var subscriber: Option[HiggsServer] = None
  /**
   * may look overly complex but is quite simple... topic =>{SUBSCRIBRS=>[(id,function),(id,function)]}
   * i.e. the key for the outter hashmap is the topic. The list buffer for each topic contains a set of functions and their IDs...
   */
  protected val listeners = new HashMap[String, ListenersList]()
  @BeanProperty
  var host = "127.0.0.1"
  @BeanProperty
  var port = 2012

  socketType match {
    case HiggsConstants.HIGGS_SUBSCRIBER => {}
    case HiggsConstants.HIGGS_PUBLISHER => {}
    case HiggsConstants.SOCKET_OTHER => {}
    case _ => {
      throw IllegalSocketTypeException("A Higgs instance can be of socket type " +
        "HiggsConstants.SOCKET_(CLIENT|SERVER|OTHER). If you need a custom type specify " +
        "socketType as being HiggsConstants.SOCKET_OTHER and set your custom encoder,decoder,client" +
        " handler and server handler")
    }
  }

  /**
   * Creates an instance which will bind or connect to the given host & port
   */
  def this(socketType: HiggsConstants.Value, h: String, p: Int) = {
    this(socketType)
    host = h
    port = p
  }

  /**
   * Binds to the given host and port
   * @throws UnsupportedOperationException if   socketType IS CLIENT
   */
  def bind() = {
    if (socketType.equals(HiggsConstants.HIGGS_PUBLISHER)) {
      throw new UnsupportedOperationException("A Higgs instance of type PUBLISHER cannot be bound, use <code>connect</code> instead")
    }
    subscriber = Some(new HiggsServer(host, port, decoder, encoder, serverHandler, new MessageListener() {
      def onMessage(m: Message) = {
        publish(m)
      }
    }))
  }

  def stop() = {
    if (socketType.equals(HiggsConstants.HIGGS_PUBLISHER)) {
      publisher.get.shutdown()
    } else {
      subscriber.get.channel.unbind()
      subscriber.get.shutdown()
    }
  }

  /**
   * Binds to the given host and port
   * @throws UnsupportedOperationException if socketType is not PUBLISHER
   */
  def connect() = {
    if (socketType.equals(HiggsConstants.HIGGS_SUBSCRIBER)) {
      throw new UnsupportedOperationException("A Higgs instance of type SUBSCRIBER cannot connect, use <code>bind</code> instead")
    }
    //wire everything together
    publisher = Some(new HiggsClient(host, port, decoder, encoder, clientHandler))
  }

  /**
   * Subscribes to a given topic, passing each message to the function provided
   * for e.g.
   * <code>
   * subscribe("stop")({
      case message => println(message)
    })
   * </code>
   * In the above example message will always be of type Message
   * get the list of functions to call for a topic
   * if the topic doesn't exist yet, create it with a new list and add the current function fn
   * if the topic exists, get the existing list of functions and append the function fn to that list
   * @param topic The topic to subscribe tp
   * @param fn The function to call for each message that matches the subscribed topic
   */
  def subscribe(topic: String)(fn: Function1[Message, Unit]) = {
    if (socketType.equals(HiggsConstants.HIGGS_SUBSCRIBER)) {
      val subscriberz = listeners.getOrElseUpdate(topic, new ListenersList())
      subscriberz.append(fn)
    } else {
      throw new UnsupportedOperationException("Only Higgs instances of type HIGGS_CLIENT can be used to subscribe to messages")
    }
  }

  /**
   * Subscribe to all messages, regardless of the topic
   */
  def receive(fn: Function1[Message, Unit]) = {
    subscribe(HiggsConstants.TOPIC_ALL)(fn)
  }

  /**
   * Attempts to send a message.
   * @return true if written successfully, false otherwise
   */
  def send(msg: String): Boolean = {
    val m = message.newInstance()
    m.setContents(msg)
    send(m)
  }

  /**
   * Attempts to send a message with the given topic.
   * @return true if written successfully, false otherwise
   */
  def send(topic: String, msg: String): Boolean = {
    val m = message.newInstance()
    m.setContents(msg)
    m.topic = topic
    send(m)
  }

  /**
   * Attempts to send a message.
   * @return true if written successfully, false otherwise
   */
  def send(msg: Message): Boolean = {
    if (socketType.equals(HiggsConstants.HIGGS_SUBSCRIBER)) {
      throw new UnsupportedOperationException("Higgs instances of type SUBSCRIBER cannot be used to send messages")
    }
    val channelObj = publisher.get.channel
    if (channelObj.isWritable) {
      channelObj.write(msg)
      true
    } else {
      false
    }
  }

  /**
   * Used by higgs internally to send a message to all subscribed topics.
   * Exposed to allow the possibility of sending messages to local subscribers of the message's topic
   */
  def publish(m: Message) = {
    //get all subscribers who want to receive all messsages
    listeners.get(HiggsConstants.TOPIC_ALL) match {
      case functions: Option[ListenersList] => {
        functions.getOrElse(List()) foreach {
          function => function(m)
        }
      }
    }
    //if the topic of the message is not set then it would have been
    //sent above, don't resend it
    if (!m.topic.equals(HiggsConstants.TOPIC_ALL.toString)) {
      //explicitly call toString comprison will fail otherwise
      //get subscribers of the message's topic and send them the message
      listeners.get(m.topic) match {
        case functions: Option[ListenersList] => {
          functions.getOrElse(List()).foreach {
            function => function(m)
          }
        }
      }
    }
  }

  /**
   * Unsubscribed a function from receiving messages about a topic.
   * @param topic the topic the function was subscribed to
   * @param id the position at which the function was subscribed. For e.g. if you have to functions
   * subscribed to two topics or even to the same topic. The ID of the first function is 0, the ID of the
   * second function is 1 and so on and so fourth
   * @return true if the function was found and removed, false otherwise
   */
  def unsubscribe(topic: String, id: Int): Boolean = {
    listeners.get(topic) match {
      case functions: Option[ListenersList] => {
        functions.get.remove(id)
        true
      }
      case _ => false //no subscribers to this topic so nothing was un subscribed
    }
  }

  /**
   * @return the total number of unique topics currently subscribed to.
   * i.e. if you subscribe to topics, a,b,b,c this method will return 3 since the two subscricptions
   * to be are just grouped under that topic
   */
  def topicsTotal() = listeners.size

  /**
   * Get a list of all topics currently subscribed to
   */
  def topics() = listeners.keys

  /**
   * Get all the functions subscribed to the given topic
   */
  def subscribersOf(topic: String) = listeners.get(topic).get

}