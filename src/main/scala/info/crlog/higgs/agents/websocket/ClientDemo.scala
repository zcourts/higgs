package info.crlog.higgs.agents.websocket

import client.WebSocketClient
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.channel.ChannelFuture


/**
 * Courtney Robinson <courtney@crlog.info>
 */
object ClientDemo {
  def main(args: Array[String]) {
    val port: Int = 7000
    val client = new WebSocketClient("localhost", port)
    client.connect((future: ChannelFuture) => {
      client.channel.write(new PingWebSocketFrame())
    })
    //    client.listen(classOf[Me], (i: Me) => {
    //      println("me:" + i.name)
    //    })
    //    client.send(new Me("huh"))
  }

}
