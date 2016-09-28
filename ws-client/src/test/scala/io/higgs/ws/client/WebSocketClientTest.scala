package io.higgs.ws.client

import org.scalatest.FlatSpec

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
class WebSocketClientTest extends FlatSpec {
  it should "be able to echo" in {
  }
}

//object Main {
//  def main(args: Array[String]) {
//    val ws = WebSocketClient(new URI("ws://echo.websocket.org"))
//    ws.onMessage((msg: String) => {
//      println(msg)
//    })
//    Thread.sleep(3000)
//    ws.send("Hello there".getBytes())
//  }
//}
