package info.crlog.higgs

import org.junit.Test
import org.junit.Assert._

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 02/02/12
 */

class HiggsTest {
  @Test
  def subscribeTest() {
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
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
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
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
    val client = new Higgs(HiggsConstants.SOCKET_CLIENT)
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
}