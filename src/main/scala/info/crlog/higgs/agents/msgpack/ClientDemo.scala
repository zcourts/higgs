package info.crlog.higgs.agents.msgpack

import io.netty.channel.ChannelFuture

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object ClientDemo {
  def main(arg: Array[String]) {
    val client = new MsgpackClient("localhost", 9090)
    client.connect((future: ChannelFuture) => {
      println("client conntected")
      client.listen(classOf[Me], (msg: Me) => {
        println(msg.name)
      })
      client.send(new Me("Test"))
    })

  }
}
