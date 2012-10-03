package info.crlog.higgs.agents.omsg

import io.netty.channel.ChannelFuture



/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object ServerDemo {
  def main(arg: Array[String]) {
    val server = new OMsgServer("localhost", 9099)
    server.bind((future: ChannelFuture) => {
      println("server bound")
    })
    server.listen(classOf[IllegalStateException], (msg: IllegalStateException) => {
      println(msg.getMessage)
      for (i <- 1 to 10) {
        server.broadcast(new NullPointerException("" + i))
      }
    })
  }
}
