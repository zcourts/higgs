package info.crlog.higgs.omsg

import io.netty.channel.Channel
import info.crlog.higgs.messages.JsonMessage

class Test(m: String) extends NullPointerException(m)

//with Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object OMsgDemo {
  def main(args: Array[String]) {
    val server = new OMsgServer("localhost", 1010)
    val client = new OMsgClient("localhost", 1010)
    server.listen((c: Channel, msg: AnyRef) => {
      println("Server all", msg)
    })
    server.listen(classOf[JsonMessage], (c: Channel, msg: JsonMessage) => {
      println("Server JsonMessage", msg)
      msg.data += "response" -> "server"
      //server.respond(c, msg)
    })
    client.listen(classOf[JsonMessage], (c: Channel, msg: JsonMessage) => {
      println("Client JsonMessage", msg)
      msg.data += "response" -> "client"
      client.respond(c, msg)
    })
    server.listen(classOf[String], (c: Channel, msg: String) => {
      println("Server topic String", msg)
    })
    server.bind(() => {
      //when server is bound connect client
      client.connect(() => {
        //when client connected send message
        client.send(JsonMessage("b", Map("huh" -> 12345, "b" -> "bang")))
        client.send(new String("Omsg Test exception"))
        server.broadcast(JsonMessage("client"))
      })
    })
  }
}
