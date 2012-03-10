package info.crlog.higgs

import api.{HiggsServer, HiggsConstants}
import protocol.{MessageListener, Message}
import collection.mutable.HashMap

class BindAgent extends HiggsAgent {
  socketType = HiggsConstants.HIGGS_SUBSCRIBER

  /**
   * may look overly complex but is quite simple... topic =>{SUBSCRIBRS=>[(id,function),(id,function)]}
   * i.e. the key for the outter hashmap is the topic. The list buffer for each topic contains a set of functions and their IDs...
   */
  protected val listeners = new HashMap[String, ListenersList]()

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
  def subscribe(topic: String)(fn: (Message) => Unit) {
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
  def receive(fn: (Message) => Unit) {
    subscribe(HiggsConstants.TOPIC_ALL)(fn)
  }

  /**
   * Binds to the given host and port
   * @throws UnsupportedOperationException if   socketType IS CLIENT
   */
  def bind() {
    if (socketType.equals(HiggsConstants.HIGGS_PUBLISHER)) {
      throw new UnsupportedOperationException("A Higgs instance of type PUBLISHER cannot be bound, use <code>connect</code> instead")
    }
    subscriber = Some(new HiggsServer(host, port, decoder, encoder, serverHandler, new MessageListener() {
      def onMessage(m: Message) {
        publish(m)
      }
    }))
  }

  /**
   * Used by higgs internally to send a message to all subscribed topics.
   * Exposed to allow the possibility of sending messages to local subscribers of the message's topic
   */
  def publish(m: Message) {
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
