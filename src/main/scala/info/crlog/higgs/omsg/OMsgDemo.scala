package info.crlog.higgs.omsg

import io.netty.channel.Channel
import info.crlog.higgs.messages.JsonMessage

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object OMsgDemo {
  def main(args: Array[String]) {
    val server = new WebSocketServer("localhost", 1010)
    val client = new WebSocketClient("localhost", 1010)
    server.listen((c: Channel, msg: JsonMessage) => {
      println("Server all", msg)
    })
    client.listen((c: Channel, msg: JsonMessage) => {
      println("Client all", msg)
      msg.data += "response" -> "client"
      msg.respond(msg)
    })
    server.listen("a", (c: Channel, msg: JsonMessage) => {
      println("Server topic a", msg)
      msg.data += "response topic" -> "a"
      msg.respond(msg)
    })
    server.bind(() => {
      //when server is bound connect client
      client.connect(() => {
        //when client connected send message
        client.send(JsonMessage("b", Map("huh" -> 12345, "b" -> "bang")))
        client.send(JsonMessage("a"))
        server.broadcast(JsonMessage("client"))
      })
    })
  }
}
