package info.crlog.higgs.protocols.omsg

import io.netty.channel.Channel
import info.crlog.higgs.protocols.websocket.JsonMessage

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object OMsgDemoClient {
  def main(args: Array[String]) {
    val client = new SerializedMsgClient("OMSG Server Demo", 1010)
    client.listen(classOf[JsonMessage], (c: Channel, msg: JsonMessage) => {
      println("Client JsonMessage", msg)
      msg.data += "response" -> "client"
      client.respond(c, msg)
    })
    client.listen(classOf[String], (c: Channel, s: String) => {
      println("string:", s)
    })
    val req = client.prepare("boom", (c: Channel, m: Integer) => {
      println("response:", m)
    }, classOf[Integer])
    client.subscribe(req, (c: Channel, m: String) => {
      println("response(string)", m)
    }, classOf[String])

    client.connect(() => {
      //      client.send(JsonMessage("b", Map("huh" -> 12345, "b" -> "bang")))
      println("before")
      req.send()
      println("after")
    })
  }
}
