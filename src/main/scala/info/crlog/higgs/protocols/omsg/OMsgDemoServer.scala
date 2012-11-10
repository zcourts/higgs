package info.crlog.higgs.protocols.omsg

import io.netty.channel.{ChannelHandlerContext, Channel}
import info.crlog.higgs.Event
import info.crlog.higgs.protocols.websocket.JsonMessage

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object OMsgDemoServer {
  def main(args: Array[String]) {
    val server = new SerializedMsgServer("localhost", 1010)
    server.listen((c: Channel, msg: AnyRef) => {
      println("Server all", msg)
    })
    server.listen(classOf[JsonMessage], (c: Channel, msg: JsonMessage) => {
      println("Server JsonMessage", msg)
      msg.data += "response" -> "server"
      //server.respond(c, msg)
    })
    server.listen(classOf[String], (c: Channel, msg: String) => {
      println("Server topic String", msg)
    })
    server ++(Event.CHANNEL_ACTIVE, (ctx: ChannelHandlerContext, c: Option[Throwable]) => {
      server.broadcast("raw string")
    })

    server.listen(classOf[String], (msg: OMsg[String]) => {
      println("server:", msg)
      msg.respond(1)
      msg.respond("server string response")
    })
    server.bind(() => {
    })
  }
}
