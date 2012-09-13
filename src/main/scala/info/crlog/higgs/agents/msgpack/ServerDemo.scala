package info.crlog.higgs.agents.msgpack

import io.netty.channel.ChannelFuture

case class Me() extends Interaction {
  var name = ""

  def this(n: String) = {
    this()
    name = n
  }
}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object ServerDemo {
  def main(arg: Array[String]) {
    val server = new MsgpackServer("localhost", 9090)
    server.bind((future: ChannelFuture) => {
      println("server bound")
    })
    server.listen(classOf[Me], (msg: Me) => {
      println(msg.name)
      for (i <- 1 to 10) {
        server.broadcast(new Me("" + i))
      }
    })
  }
}
