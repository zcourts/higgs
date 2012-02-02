package info.crlog.higgs

import protocol.boson._
import protocol.Message
import reflect.BeanProperty
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import collection.mutable.{ListBuffer, HashMap}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class Higgs[T](var socketType: HiggsConstants.Value) {
  /**
   * The decoder Higgs uses to decode messages
   */
  @BeanProperty
  var decoder: Option[FrameDecoder] = None
  //default protocol encoder
  @BeanProperty
  var encoder: Option[OneToOneEncoder] = None
  //default client request handler
  @BeanProperty
  var clientHandler: Option[SimpleChannelUpstreamHandler] = None
  //default server request handler
  @BeanProperty
  var serverHandler: Option[SimpleChannelUpstreamHandler] = None
  //avoid matching socketType multiple times to determine if we're a client, server or other...
  private var isClient = false
  //
  //private val listeners = new ListBuffer[Function1]
  //
  private var client: Option[HiggsClient] = None
  private var server: Option[HiggsServer] = None
  /**
   * may look overly complex but is quite simple...
   * topic =>{SUBSCRIBRS=>[(id,function),(id,function)]}
   * i.e. the key for the outter hashmap is the topic.
   * The list buffer for each topic contains a set of functions and their IDs...
   */
  private val listeners = new HashMap[String, ListBuffer[Tuple2[Int, Function1[Message, Unit]]]]()

  socketType match {
    case HiggsConstants.SOCKET_CLIENT => {
      initBosonEncoderAndDecoder
      clientHandler = Some(new ClientHandler)
      isClient = true
    }
    case HiggsConstants.SOCKET_SERVER => {
      initBosonEncoderAndDecoder
      serverHandler = Some(new ServerHandler)
    }
    case HiggsConstants.SOCKET_OTHER => {}
    case _ => {
      throw IllegalSocketTypeException("A Higgs instance can be of socket type " +
        "HiggsConstants.SOCKET_(CLIENT|SERVER|OTHER). If you need a custom type specify " +
        "socketType as being HiggsConstants.SOCKET_OTHER and set your custom encoder,decoder,client" +
        " handler and server handler")
    }
  }


  /**
   * Binds to localhost on port 2012
   * @throws UnsupportedOperationException if socketType is CLIENT
   */
  def bind() {
    bind("localhost", 2012)
  }

  /**
   * Binds to the given host and port
   * @throws UnsupportedOperationException if   socketType IS CLIENT
   */
  def bind(host: String, port: Int) = {
    if (isClient) {
      throw new UnsupportedOperationException("A Higgs instance of type CLIENT cannot be bound, use <code>connect</code> instead")
    }
  }

  /**
   * Connects a client to localhost,2012
   * @throws UnsupportedOperationException if socketType is not CLIENT
   */
  def connect() {
    connect("localhost", 2012)
  }

  /**
   * Binds to the given host and port
   * @throws UnsupportedOperationException if socketType is not CLIENT
   */
  def connect(host: String, port: Int) = {
    if (!isClient) {
      throw new UnsupportedOperationException("A Higgs instance of type " + socketType + " cannot connect, use <code>bind</code> instead")
    }
    client = Some(new HiggsClient(host, port))
    //client.get.
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
    val subscriberz = listeners.getOrElseUpdate(topic, new ListBuffer[Tuple2[Int, Function1[Message, Unit]]]())
    subscriberz.append((subscriberz.size, fn))
  }

  /**
   * Subscribe to all messages, regardless of the topic
   */
  def receive(fn: Function1[Message, Unit]) = {
    subscribe("")(fn)
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

  /**
   * initialize the encoder and decoder for Boson
   */
  private def initBosonEncoderAndDecoder {
    decoder = Some(new BosonDecoder)
    encoder = Some(new BosonEncoder)
  }
}