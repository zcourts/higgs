# Higgs Boson

The name shamelessly stolen from the almost mythical Higgs Boson http://en.wikipedia.org/wiki/Higgs_boson .

* __Higgs__ - the name of the library
* __Boson__ - the name of the protocol, the protocol specification is in the "info.crlog.higgs.protocols.boson" package

Together with __Netty__ forms a pure JVM (NIO based) high performance, message oriented networking library.
The project was started to remove the need I had for ZeroMQ (too many issues with native dependency in jzmq)

It has since grown to be more robust than originally intended.

# Supported protocols

1. HTTP/HTTPS (Client) - Server to be added
2. WebSocket  (Client and Server) - Compatible with Socket.io or any other WebSocket client
3. Boson - A language independent, topic oriented protocol for sending/receiving arbitrary data over the network and
            performing remote method invocation (RMI/RPC).

# Custom Protocols
+ One of the biggest wins with Higgs is it provides a simple infrastructure for you to do your own protocol.
            The [advanced](#advanced) section below shows how trivial it is to do so.
            However, as it stands, Higgs will continue to add support for "standard" protocols. On the to do list are
            ftp,ssh, sctp, telnet etc.


+ __Boson__ is the actively used/developed protocol and is the only custom protocol that is recommended for use.
+ A __Node JS__ implementation of the Boson protocol can be found here [https://github.com/zcourts/higgs-node](https://github.com/zcourts/higgs)
+ The Boson protocol is actively used between Node JS and Scala at [Fillta](http://fillta.com)
+ The library uses the latest version 4 Netty API. Since the netty project had a major refactor between v3 and v4
it is not compatible with previous versions and Netty needs to be built and installed in your local maven REPO using the [master branch](https://github.com/netty/netty)
+ TODO: See if replacing reflection based invocation with code generation via [ReflexASM](http://code.google.com/p/reflectasm/) makes a diff.
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

package info.crlog.higgs.protocols.http

import java.net.URL
import java.nio.file.Files

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object HttpDemo {
  def main(args: Array[String]) {
    val file = Files.createTempFile("higgs.test", ".tmp").toFile()
    val client = new HttpRequestBuilder()
    //    for (x <- 1 to 100) {
    client.query("a", "b")
      //        .query("c", x)
      .cookie("c", "d")
      .cookies(Map("age" -> 100)) //or we can do
      .header("X-val", "yes")
      .headers(Map("X-a" -> 123, "X-b" -> "val"))
      .compress(true)
      .url(new URL("https://httpbin.org/delete"))
      .DELETE() //http DELETE request
      //build request and send
      .build((r) => {
      println(r) //print response
    })
      .url(new URL("https://httpbin.org/get"))
      .GET()
      .build((r) => {
      println(r)
    })
      .url(new URL("https://httpbin.org/post"))
      .POST()
      //upload a single file
      .file(new HttpFile("post.txt", file))
      //upload multiple files under the same name
      .file("my-var", List(new PartialHttpFile(file), new PartialHttpFile(file)))
      //or upload multiple files each with different names
      .file(List(new HttpFile("file-1", file), new HttpFile("file-2", file)))
      //use form to supply normal form field data i.e. none binary form fields
      .form("name", "Courtney")
      .build((r) => {
      println(r)
    })
    //TODO add PUT support
//      .url(new URL("https://httpbin.org/put"))
//      .PUT()
//      .form("name", "Courtney Robinson")
//      .build((r) => {
//      println(r)
//    })
    //notice all previous settings on the builder is kept and goes into the next request
    //if you add files for e.g. and do a POST request then do a GET only settings supported by
    //an HTTP GET request is used. to discard all previous settings use .clear() e.g.
    .clear() //now everything set previously has been discarded and a clean/new builder is returned
    .GET() //etc...
    //    }
  }
}

```
Output:

```javascript

200 OK
 SINGLE
 HTTP/1.1
 Map(Connection -> ListBuffer(Close), Server -> ListBuffer(gunicorn/0.13.4), Date -> ListBuffer(Sat, 01 Dec 2012 16:01:52 GMT), Content-Type -> ListBuffer(application/json), Content-Length -> ListBuffer(696))
 {
  "origin": "2.122.227.229",
  "headers": {
    "Content-Length": "",
    "Accept-Language": "en",
    "Accept-Encoding": "gzip,deflate",
    "Host": "httpbin.org",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "User-Agent": "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)",
    "Accept-Charset": "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
    "Connection": "keep-alive",
    "X-A": "123",
    "Referer": "https://httpbin.org/delete",
    "X-B": "val",
    "X-Val": "yes",
    "Cookie": "c=d; age=100",
    "Content-Type": ""
  },
  "json": null,
  "url": "http://httpbin.org/delete?a=b",
  "args": {
    "a": "b"
  },
  "data": ""
}

200 OK
 SINGLE
 HTTP/1.1
 Map(Connection -> ListBuffer(Close), Server -> ListBuffer(gunicorn/0.13.4), Date -> ListBuffer(Sat, 01 Dec 2012 16:01:52 GMT), Content-Type -> ListBuffer(application/json), Content-Length -> ListBuffer(660))
 {
  "url": "http://httpbin.org/get?a=b",
  "headers": {
    "Content-Length": "",
    "Accept-Language": "en",
    "Accept-Encoding": "gzip,deflate",
    "Host": "httpbin.org",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "User-Agent": "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)",
    "Accept-Charset": "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
    "Connection": "keep-alive",
    "X-A": "123",
    "Referer": "https://httpbin.org/get",
    "X-B": "val",
    "X-Val": "yes",
    "Cookie": "c=d; age=100",
    "Content-Type": ""
  },
  "args": {
    "a": "b"
  },
  "origin": "2.122.227.229"
}

200 OK
 SINGLE
 HTTP/1.1
 Map(Connection -> ListBuffer(Close), Server -> ListBuffer(gunicorn/0.13.4), Date -> ListBuffer(Sat, 01 Dec 2012 16:01:52 GMT), Content-Type -> ListBuffer(application/json), Content-Length -> ListBuffer(1465))
 {
  "origin": "2.122.227.229",
  "files": {
    "post.txt": "",
    "file-2": "\r\nContent-Disposition: form-data; name=\"my-var\"\r\nContent-Type: multipart/mixed; boundary=545ffa43cd80502e\r\n\r\n--545ffa43cd80502e\r\nContent-Disposition: file; filename=\"higgs.test2350892187396857516.tmp\"\r\nContent-Disposition: form-data; name=\"my-var\"; filename=\"higgs.test2350892187396857516.tmp\"\r\nContent-Type: application/octet-stream\r\nContent-Transfer-Encoding: binary\r\n\r\n\r\n--545ffa43cd80502e\r\nContent-Disposition: file; filename=\"higgs.test2350892187396857516.tmp\"\r\nContent-Type: application/octet-stream\r\nContent-Transfer-Encoding: binary\r\n\r\n\r\n--545ffa43cd80502e--",
    "file-1": ""
  },
  "form": {
    "name": "Courtney"
  },
  "url": "http://httpbin.org/post?a=b",
  "args": {
    "a": "b"
  },
  "headers": {
    "Content-Length": "1272",
    "Accept-Language": "en",
    "Accept-Encoding": "gzip,deflate",
    "Host": "httpbin.org",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "User-Agent": "Mozilla/5.0 (compatible; HiggsBoson/0.0.1; +https://github.com/zcourts/higgs)",
    "Accept-Charset": "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
    "Connection": "keep-alive",
    "X-A": "123",
    "Referer": "https://httpbin.org/post",
    "X-B": "val",
    "X-Val": "yes",
    "Cookie": "c=d; age=100",
    "Content-Type": "multipart/form-data; boundary=67b1eeaaf8430d47"
  },
  "json": null,
  "data": ""
}

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

# Metrics

Some Metrics are published by the library to help you figure out what's going on in prod.
Below are some ScreenShots of available metrics.

![Metric 1](higgs/metric1.png)

![Metric 2](higgs/metric2.png)

![Metric 3](higgs/metric3.png)

![Metric 4](higgs/metric4.png)

![Metric 5](higgs/metric5.png)