package io.higgs.ws.client

import java.net.URI

import org.scalatest.FlatSpec

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
class WebSocketClientTest extends FlatSpec {
  it should "be able to echo" in {
  }
}

object Main {
  def main(args: Array[String]) {
    val ws = WebSocketClient(new URI("wss://echo.websocket.org"))
    ws.onMessage((msg: String) => {
      println(s"$msg")
    })
    Thread.sleep(5000)
    ws.send("Hello there")
    var i: Int = 1
    ws.onConnect(() => {
      i += 1
      ws.send(s"Hello there $i")
    })
  }
}
