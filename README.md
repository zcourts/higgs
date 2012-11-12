# Higgs Boson

The name shamelessly stolen from the almost mystical Higgs Boson http://en.wikipedia.org/wiki/Higgs_boson .

* __Higgs__ - the name of the library
* __Boson__ - the name of the protocol, the protocol specification is in the "info.crlog.higgs.protocols.boson" package

Together with __Netty__ forms a pure JVM (NIO based) high performance, message oriented networking library.
The project was started to remove the need I had for ZeroMQ (too many issues with native dependency in jzmq)

It has since grown to be more robust that originally intended.

# Supported protocols

1. HTTP/HTTPS (Client) - Server to be added
2. WebSocket  (Client and Server) - Compatible with Socket.io or any other WebSocket client
3. OMSG - A JVM based, topic protocol for sending arbitrary objects over the network
4. JRPC - A custom RPC mechanism for JVM languages (Uses object serialization)
5. Boson - A language independent, topic oriented protocol for sending/receiving arbitrary data over the network and
            performing remote method invocation (RMI/RPC).

NOTE: JRPC and OMSG protocols are highly experimental and not intended for general use (They were more for demonstration).
		+ __Boson__ is the actively used/developed protocol and is the only custom protocol that is recommended for use.
		+ A __Node JS__ implementation of the Boson protocol can be found here [https://github.com/zcourts/higgs-node](https://github.com/zcourts/higgs)
		+ The Boson protocol is actively used between Node JS and Scala at [Fillta](http://fillta.com)

# Features

* Simplicity and Abstraction from the underlying NIO operations & socket handling.
* Extensible - Allowing user supplied protocols, encoders,decoders,client server handlers
* Performant
* Easily extensible to add custom protocols, binary or otherwise. OMSG and JRPC are intended as a demonstration of how
	easy it is to do custom protocols.
* Built on top of [Netty](http://netty.io)

# Getting started

Each protocol comes with a simple client/server demo.

## Examples

### HTTP

```scala

object HttpDemo {
  def main(args: Array[String]) {
    val client = new HttpClient()
    client.GET(new URL("https://api.twitter.com/1.1/statuses/home_timeline.json"), (res: HTTPResponse) => {
      println("Twitter")
      println(res)
    })
  }
}

```
Output:

```javascript

Twitter
400 Bad Request
 SINGLE
 HTTP/1.1
 Map(Server -> ListBuffer(tfe), Date -> ListBuffer(Mon, 12 Nov 2012 00:09:06 UTC), Content-Type -> ListBuffer(application/json; charset=utf-8), Content-Length -> ListBuffer(61))
 {"errors":[{"message":"Bad Authentication data","code":215}]}

```
### Boson  [Protocol Specification](https://github.com/zcourts/higgs/tree/master/src/main/scala/info/crlog/higgs/protocols/boson)

```scala

class Listener {
  @method("test")
  def test(a: Double, str: String) = {
    println(a, str)
    a * math.random
  }
}

object DemoClient {
  def main(args: Array[String]) {
    val server = new BosonServer(12001)
    server.register(new Listener)
    server.bind()
    val client = new BosonClient("BosonTest", 12001)
    client.connect()
    for (i <- 1 to 10) {
      client.invoke("test", Array(math.random * i, "random"), (m: Double) => {
        println("received:", m)
      }, false)
    }
  }
}

```
Output

```javascript

(0.6504514338228282,random)
(received:,0.13370158985585276)
(0.5559087056550658,random)
(2.264004979117151,random)
(received:,0.13683075122251703)
(1.0331329639981006,random)
...

```

# Advanced

Higgs is a fairly flexible library.

Here's a quick protocol implementation  creates a server and client.
When the client is connected to the server it sends the number 12345,
When the server receives this it responds with 67890

```scala

//See this example in the package below
package info.crlog.higgs.protocols

import info.crlog.higgs.{Serializer, Server, Client}
import io.netty.handler.codec.{ByteToMessageDecoder, MessageToByteEncoder}
import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.{Unpooled, ByteBuf}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object Readme {
  def main(args: Array[String]) {
    val server = new MyServer(9090)
    val client = new MyClient("Test Server", 9090)
    server.bind()
    client.connect(() => {
      client.send(12345)
    })
  }
}

class MyEncoder extends MessageToByteEncoder[Array[Byte]] {
  def encode(ctx: ChannelHandlerContext, msg: Array[Byte], out: ByteBuf) {
    out.writeBytes(msg)
  }
}

class MyDecoder extends ByteToMessageDecoder[Array[Byte]] {
  def decode(ctx: ChannelHandlerContext, buffer: ByteBuf): Array[Byte] = {
    // Wait until an int is available, int = 4 bytes
    if (buffer.readableBytes < 4) {
      return null
    }
    buffer.resetReaderIndex()
    val messageContents: Array[Byte] = new Array[Byte](4)
    buffer.readBytes(messageContents)
    messageContents
  }
}

class MySerializer extends Serializer[Int, Array[Byte]] {
  def serialize(obj: Int) = {
    val buf = Unpooled.copyInt(obj)
    val arr = new Array[Byte](buf.writerIndex())
    buf.getBytes(0, arr, 0, buf.writerIndex())
    arr
  }

  def deserialize(obj: Array[Byte]) = Unpooled.copiedBuffer(obj).readInt()
}

class MyServer(port: Int, host: String = "localhost", compress: Boolean = true)
  extends Server[String, Int, Array[Byte]](host, port, compress) {
  val serializer = new MySerializer()

  def decoder() = new MyDecoder()

  def encoder() = new MyEncoder()

  def allTopicsKey(): String = ""

  def broadcast(obj: Int) {
    //TODO
  }

  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val data = serializer.deserialize(value)
    println("Server received", data)
    respond(context.channel(), 67890)
  }
}

class MyClient(serviceName: String, port: Int, host: String = "localhost", compress: Boolean = true)
  extends Client[String, Int, Array[Byte]](serviceName, port, host, compress) {
  val serializer = new MySerializer()

  def decoder() = new MyDecoder()

  def encoder() = new MyEncoder()

  def allTopicsKey(): String = ""

  def message(context: ChannelHandlerContext, value: Array[Byte]) {
    val data = serializer.deserialize(value)
    println("Client received", data)
    System.exit(0)
  }
}

```