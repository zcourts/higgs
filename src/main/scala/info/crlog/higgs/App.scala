package info.crlog.higgs


object App {
  def main(args: Array[String]) = {
    val subscriber = new Higgs(HiggsConstants.HIGGS_SUBSCRIBER)
    subscriber.port = 9090
    //get all messages regardless of topic
    subscriber.receive {
      message => println("App: " + message)
    }
    //sub scribe to the topic 'a'
    //    subscriber.subscribe("a") {
    //      case message: BosonMessage => println(message)
    //      case message: Message => println(message)
    //    }
    subscriber.bind()

    val publisher = new Higgs(HiggsConstants.HIGGS_PUBLISHER)
    publisher.port = 9090
    publisher.connect()
    val then = System.currentTimeMillis()
    for (i <- 0 to 1000) {
      publisher send "Message " + i
    }
    val now = System.currentTimeMillis()
    Thread.sleep(1000)
    println("Stopping")
    println("Been :" + (now - then) + " mili secs")
    subscriber.stop
    publisher.stop
    System.exit(0)
  }
}
