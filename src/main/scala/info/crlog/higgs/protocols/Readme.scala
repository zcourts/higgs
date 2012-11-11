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