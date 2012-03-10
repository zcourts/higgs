package info.crlog.higgs

import java.util.Date

/**
 * TODO: Add docs
 */

object AgentDemo {
  def main(args: Array[String]) {
    val host = "192.168.0.17"
    new Thread(new Runnable() {
      def run() {
        val subscriber = new BindAgent
        subscriber.host = host
        subscriber.bind()
        var start = new Date
        var end = new Date
        subscriber.receive {
          msg => {
            if (msg.toString.toInt == 0) {
              start = new Date
            }
            if (msg.toString.toInt == 1000000) {
              end = new Date
              println("Took " + ((end.getTime - start.getTime) ) + " milli seconds to receive 1M messages")
            }
          }
        }
      }
    }).start
    Thread.sleep(10000)
    val publisher = new ConnectAgent
    publisher.host = host
    publisher.connect()
    println(new Date)
    for (j <- 0 to 1000000000) {
      publisher.send(j.toString)
    }
    println(new Date)
    publisher.stop()
    //System.exit(0)
  }
}
