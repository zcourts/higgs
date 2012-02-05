package info.crlog.higgs

import info.crlog.higgs.protocol.boson.BosonMessage
import info.crlog.higgs.protocol.Message


object App {
  def main(args: Array[String]) = {
    val subscriber = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    subscriber.port = 9090
    //get all messages regardless of topic
    subscriber.receive {
      message => println("App: "+message)
    }
    //sub scribe to the topic 'a'
//    subscriber.subscribe("a") {
//      case message: BosonMessage => println(message)
//      case message: Message => println(message)
//    }
    subscriber.bind()

    val publisher= new Higgs(HiggsConstants.HIGGS_PUBLISHER)
    publisher.port = 9090
    publisher.connect()

    for (i <- 0 to 10){
        publisher send  "Message "+i
    }
  }
}
