package info.crlog.higgs

import org.junit.Test
import org.junit.Assert._
import protocol.boson.BosonMessage
import protocol.Message

/**
 * {class A; class B extends A; def foo(a: A) = {a match {case b:B => println("Matched B");case aa:A => println("Matched A")}}; foo(new B);foo(new A)}
 * As in the above, to message can be of type Message or any subclass there of.
 * @author Courtney Robinson <courtney@crlog.info> @ 02/02/12
 */
class HiggsTest {
  @Test
  def differentMessageTypes() {
    var client = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    var boson = 0
    var m = 0
    client.receive {
     case message: BosonMessage => boson += 1
      case message: Message => m += 1
    }
    client.publish(new Message() {})
    client.publish(new BosonMessage(""))
    assertEquals(1, boson)
    assertEquals(1, m)
  }

  @Test
  def messageMultiPublishing() {
    val client = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    var received = 0
    var subscribed = 0
    client receive {
      message => {
        received += 1
      }
    }
    client receive {
      message => {
        received += 1
      }
    }
    val topic = "a"
    client.subscribe(topic) {
      message => {
        assertEquals(topic, message.topic)
        subscribed += 1
      }
    }
    client.subscribe(topic) {
      message => {
        assertEquals(topic, message.topic)
        subscribed += 1
      }
    }
    //same as no topic, shouldn't be sent twice to receive though
    client.publish(new Message() {})
    client.publish(new BosonMessage("b", ""))
    client.publish(new BosonMessage(topic, ""))
    client.publish(new BosonMessage(topic, ""))
    assertEquals(8, received) //should be 8, sent 4 messages but two subscribers that increment that same value
    assertEquals(4, subscribed) //should be 4, sent 3 messages with topic a, but two subscribers inc the same value+
  }

  //test receive all and topic subscription
  @Test
  def messagePublishing() {
    val client = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    var received = 0
    var subscribed = 0
    client receive {
      message => {
        received += 1
        println(message)
      }
    }
    val topic = "a"
    client.subscribe(topic) {
      message => {
        assertEquals(topic, message.topic)
        subscribed += 1
      }
    }
    //same as no topic, shouldn't be sent twice to receive though
    client.publish(new Message() {})
    client.publish(new BosonMessage("b", ""))
    client.publish(new BosonMessage(topic, ""))
    client.publish(new BosonMessage(topic, ""))
    assertEquals(4, received) //should have received all 4 messages and incremented to 4
    assertEquals(2, subscribed) //should only have received 2 messages with the topic "a"
  }

  @Test
  def subscribeAll() {
    val client = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    client.receive {
      message => println()
    }
    assertEquals(1, client.topicsTotal)
    client.receive {
      message => println()
    }
    assertEquals(1, client.topicsTotal)
    //the recieve method subscribes you to an empty string meaning everything so
    //we should have 1 topic (everything) and 2 subscribers
    assertEquals(2, client.subscribersOf(HiggsConstants.TOPIC_ALL).size)
  }

  @Test
  def subscribeTest() {
    val client = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    client.subscribe("stop") {
      message => println(message)
    }
    client.subscribe("stop") {
      message => println(message)
    }
    client.subscribe("stop") {
      message => println(message)
    }
    assertEquals(1, client.topicsTotal)
  }

  @Test
  def subscribe3TopicsTest() {
    val client = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    client.subscribe("a") {
      message => println(message)
    }
    client.subscribe("b") {
      message => println(message)
    }
    client.subscribe("c") {
      message => println(message)
    }
    assertEquals(3, client.topicsTotal)
    client.subscribe("b") {
      message => println(message)
    }
    //should still be three since c is the same topic with an extra function
    assertEquals(3, client.topicsTotal)
  }

  /**
   * Make sure multiple functions are associated with a single topic
   */
  @Test
  def multipleSubscribersTest() {
    val client = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    client.subscribe("a") {
      message => println(message)
    }
    client.subscribe("b") {
      message => println(message)
    }
    client.subscribe("b") {
      message => println(message)
    }
    client.subscribe("b") {
      message => println(message)
    }
    assertEquals(2, client.topicsTotal)
    assertEquals(3, client.subscribersOf("b").size)
  }

  @Test(expected = classOf[IllegalSocketTypeException])
  def illegalSocketTypeTest() {
    val client = new Higgs(HiggsConstants.TOPIC_ALL)
  }
}