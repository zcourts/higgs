package info.crlog.higgs

import protocol._
import protocol.boson._
import reflect.BeanProperty
import collection.mutable.{ListBuffer, HashMap}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class Higgs(var socketType: HiggsConstants.Value) {
  type MessageType = Option[_ <: Message]
  type ListenersList = ListBuffer[Function1[MessageType, Unit]]
  /**
   * The decoder Higgs uses to decode messages
   */
  @BeanProperty //default protocol decoder
  var decoder: Class[_ <: HiggsDecoder] = classOf[BosonDecoder]
  @BeanProperty //default protocol encoder
  var encoder: Class[_ <: HiggsEncoder] = classOf[BosonEncoder]
  @BeanProperty //default client request handler
  var clientHandler: Class[_ <: HiggsClientHandler] = classOf[ClientHandler]
  @BeanProperty //default server request handler
  var serverHandler: Class[_ <: HiggsServerHandler] = classOf[ServerHandler]
  var message: Class[_ <: Message] = classOf[BosonMessage]
  //avoid matching socketType multiple times to determine if we're a client, server or other...
  private var isClient = false
  private var client: Option[HiggsClient] = None
  private var server: Option[HiggsServer] = None
  /**
   * may look overly complex but is quite simple... topic =>{SUBSCRIBRS=>[(id,function),(id,function)]}
   * i.e. the key for the outter hashmap is the topic. The list buffer for each topic contains a set of functions and their IDs...
   */
  private val listeners = new HashMap[String, ListenersList]()
  @BeanProperty
  var host = "127.0.0.1"
  @BeanProperty
  var port = 2012

  socketType match {
    case HiggsConstants.SOCKET_CLIENT => {
      isClient = true
    }
    case HiggsConstants.SOCKET_SERVER => {}
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
    this (socketType)
    host = h
    port = p
  }

  /**
   * Binds to the given host and port
   * @throws UnsupportedOperationException if   socketType IS CLIENT
   */
  def bind() = {
    if (isClient) {
      throw new UnsupportedOperationException("A Higgs instance of type CLIENT cannot be bound, use <code>connect</code> instead")
    }
    server = Some(new HiggsServer(host, port))
  }

  /**
   * Binds to the given host and port
   * @throws UnsupportedOperationException if socketType is not CLIENT
   */
  def connect() = {
    if (!isClient) {
      throw new UnsupportedOperationException("A Higgs instance of type " + socketType + " cannot connect, use <code>bind</code> instead")
    }
    //wire everything together
    client = Some(new HiggsClient(host, port, decoder, encoder, clientHandler))
    client.get.handler.addListener(new MessageListener() {
      def onMessage(m: Message) = {
        publish(Some(m))
      }
    })
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
  def subscribe(topic: String)(fn: Function1[MessageType, Unit]) = {
    val subscriberz = listeners.getOrElseUpdate(topic, new ListenersList())
    subscriberz.append(fn)
  }

  /**
   * Subscribe to all messages, regardless of the topic
   */
  def receive(fn: Function1[MessageType, Unit]) = {
    subscribe(HiggsConstants.TOPIC_ALL)(fn)
  }

  /**
   * Attempts to send a message.
   * @return true if written successfully, false otherwise
   */
  def send(msg: String): Boolean = {
    val m = message.newInstance()
    m.setContents(msg.getBytes)
    send(msg)
  }

  /**
   * Attempts to send a message.
   * @return true if written successfully, false otherwise
   */
  def send(msg: Message): Boolean = {
    val channelObj = client.get.channel
    if (channelObj.isWritable) {
      channelObj.write(msg.asBytes())
      true
    } else {
      false
    }
  }

  private def publish(m: MessageType) = {
    //get all subscribers who want to receive all messsages
    listeners.get(HiggsConstants.TOPIC_ALL) match {
      case functions: Option[ListenersList] => {}
      case _ => //do nothing  in all other cases, we simply don't have anyone listening to everything
    }

    //get subscribers of the message's topic and send them the message
    listeners.get(m.get.topic) match {
      case functions: Option[ListenersList] => {
        functions.get foreach {
          function => function(m.get.asInstanceOf[MessageType])
        }
      }
      case _ => //no subscribers to this topic so discard the message
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