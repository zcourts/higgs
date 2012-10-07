package info.crlog.higgs.omsg

import io.netty.channel.Channel
import info.crlog.higgs.messages.JsonMessage

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object OMsgDemo {
  def main(args: Array[String]) {
    val server = new SerializedMsgServer("localhost", 1010)
    val client = new SerializedMsgClient("localhost", 1010)
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



    server.listen((msg: OMsg[String]) => {
      println("server:", msg)
      msg.respond(1)
      msg.respond("server string response")
    })
    client.listen(classOf[String], (c: Channel, s: String) => {
      println("string:", s)
    })
    server.bind(() => {
      client.connect(() => {
        server.broadcast("raw string")
        client.send(JsonMessage("b", Map("huh" -> 12345, "b" -> "bang")))
        val req = client.prepare("boom", (c: Channel, m: Integer) => {
          println("response:", m)
        }, classOf[Integer])
        client.subscribe(req, (c: Channel, m: String) => {
          println("response(string)", m)
        }, classOf[String])
        req.send()
      })
    })
    //    server.bind(() => {
    //      //when server is bound connect client
    //      client.connect(() => {
    //        //when client connected send message
    //        client.send(JsonMessage("b", Map("huh" -> 12345, "b" -> "bang")))
    //        client.send(new String("Omsg Test exception"))
    //        server.broadcast(JsonMessage("client"))
    //      })
    //    })
  }
}
