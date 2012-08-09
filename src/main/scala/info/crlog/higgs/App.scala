package info.crlog.higgs

import api.{Higgs, HiggsConstants}
import java.util.Date
import scala.util.Random


object App {
  def topic(): String = {
    val rand = new Random
    val min = 0
    val max = 2
    val randomNum = rand.nextInt(max - min + 1) + min;
    val topics = Array[String]("", "a", "b")
    topics(randomNum)
  }

  def main(args: Array[String]) {
    val host = "192.168.0.17"
    val stop = 1000000
    def pub() {
      val publisher = new Higgs(HiggsConstants.HIGGS_PUBLISHER)
      publisher.host = host
      publisher.port = 9090
      publisher.connect()

      println("Starting " + new Date())
      for (i <- 1 to stop) {
        //        val msg = "Sending a much much longer message this time, it should be more than a few bytes so we can get a better idea of what this crap looks like."
        publisher send("a", "b")
      }
    }


    val subscriber = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    subscriber.host = host
    subscriber.port = 9090
    var i = 0
    var now: Double = 0D
    val then: Double = System.currentTimeMillis()
    //get all messages regardless of topic
    subscriber.receive {
      message => {
        i += 1;
        now = System.currentTimeMillis();
        println(i + " ALL => " + message)
        if (i == stop) {
          println("Stopping " + new Date())
          println("Been :" + ((now - then) / 1000) + " seconds, received " + i + " thats :" + (stop / ((now - then) / 1000)) + " per second")
          System.exit(0)
        }
      }
    }
    //sub scribe to the topic 'a'
    subscriber.subscribe("a") {
      case message => //println("A listener => topic:" + message.topic + ", message:" + message)
    }
    subscriber.subscribe("b") {
      case message => //println("B listener => topic:" + message.topic + ", message:" + message)
    }
    subscriber.bind()
    pub() //start publishing
  }
}