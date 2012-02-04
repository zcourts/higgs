package info.crlog.higgs

import org.junit.Test
import org.junit.Assert._
import protocol.boson.BosonMessage

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 02/02/12
 */

class HiggsTest {
  @Test
  def subscribeAll() {
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
    client.receive {
      message: BosonMessage => println()
    }
    assertEquals(1, client.topicsTotal)
    client.receive {
      message: BosonMessage => println()
    }
    assertEquals(1, client.topicsTotal)
    //the recieve method subscribes you to an empty string meaning everything so
    //we should have 1 topic (everything) and 2 subscribers
    assertEquals(2, client.subscribersOf(HiggsConstants.TOPIC_ALL).size)
  }

  @Test
  def subscribeTest() {
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
    client.subscribe("stop") {
      message: BosonMessage => println(message)
    }
    client.subscribe("stop") {
      message: BosonMessage => println(message)
    }
    client.subscribe("stop") {
      message: BosonMessage => println(message)
    }
    assertEquals(1, client.topicsTotal)
  }

  @Test
  def subscribe3TopicsTest() {
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
    client.subscribe("a") {
      message: BosonMessage => println(message)
    }
    client.subscribe("b") {
      message: BosonMessage => println(message)
    }
    client.subscribe("c") {
      message: BosonMessage => println(message)
    }
    assertEquals(3, client.topicsTotal)
    client.subscribe("b") {
      message: BosonMessage => println(message)
    }
    //should still be three since c is the same topic with an extra function
    assertEquals(3, client.topicsTotal)
  }

  /**
   * Make sure multiple functions are associated with a single topic
   */
  @Test
  def multipleSubscribersTest() {
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
    client.subscribe("a") {
      message: BosonMessage => println(message)
    }
    client.subscribe("b") {
      message: BosonMessage => println(message)
    }
    client.subscribe("b") {
      message: BosonMessage => println(message)
    }
    client.subscribe("b") {
      message: BosonMessage => println(message)
    }
    assertEquals(2, client.topicsTotal)
    assertEquals(3, client.subscribersOf("b").size)
  }

  @Test(expected = classOf[IllegalSocketTypeException])
  def illegalSocketTypeTest(){
      val client = new Higgs(HiggsConstants.TOPIC_ALL)
  }
}