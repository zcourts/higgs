package info.crlog.higgs.agents.websocket

import server.WebSocketServer


/**
 * Courtney Robinson <courtney@crlog.info>
 */
object ServerDemo {
  def main(args: Array[String]) {
    val port: Int = 8080
    val server = new WebSocketServer("localhost", port)
    server.bind()

    server.listen(classOf[Me], (m: Me) => {
      //      m.respond(new Me("response to me"))
      for (i <- 1 to 10) {
        server.broadcast(new Me("me" + i))
        server.broadcast(new Me2("me2" + i))
      }
    })
  }
}
