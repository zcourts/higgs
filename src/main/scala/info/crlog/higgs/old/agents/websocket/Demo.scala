package info.crlog.higgs.agents.websocket

import client.WebSocketClient
import server.WebSocketServer
import io.netty.channel.ChannelFuture
import info.crlog.higgs.agents.msgpack.Interaction

case class Me() extends Interaction {
  def this(n: String) = {
    this()
    name = n
  }

  var name: String = ""
}

class Me2 extends Me {
  def this(n: String) = {
    this()
    name = n
  }
}

/**
 * Courtney Robinson <courtney@crlog.info>
 */
object Demo {
  def main(args: Array[String]) {
    val port: Int = 8080
    val server = new WebSocketServer("localhost",port)
    server.bind()
    val client = new WebSocketClient("localhost", port)
    client.connect((future: ChannelFuture) => {

    })

    //    server.listen(classOf[Me], (m: Me) => {
    //      //      m.respond(new Me("response to me"))
    //      for (i <- 1 to 10) {
    //        server.broadcast(new Me("me" + i))
    //        server.broadcast(new Me2("me2" + i))
    //      }
    //    })
    client.listen(classOf[Me], (i: Me) => {
      println("me:" + i.name)
    })
    client.send(new Me("huh"))
    //    server.listen(classOf[Me2], (m: Me2) => {
    //      m.respond(new Me2("response to me2"))
    //    })
    //    client.listen(classOf[Me2], (i: Me2) => {
    //      println("me2:" + i.name)
    //    })
    //    for (i <- 1 to 10) {
    //      client.send(new Me("me" + i))
    //      client.send(new Me2("me2" + i))
    //    }
  }

}
