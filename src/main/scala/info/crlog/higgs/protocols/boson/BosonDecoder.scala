package info.crlog.higgs.protocols.boson

import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.ByteBuf

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonDecoder extends ByteToMessageDecoder[Array[Byte]] {

  def decode(ctx: ChannelHandlerContext, buffer: ByteBuf): Array[Byte] = {

    // Wait until the protocol version and size of the message is available.
    if (buffer.readableBytes < 5) {
      return null
    }
    //set reader index to 0
    buffer.resetReaderIndex()
    val protocolVersion:Int=buffer.readByte()
    val dataLength: Int = buffer.readInt //get the data size, i.e. 4 bytes (32 bit signed java int)
    // Wait until the full message is available
    if (buffer.readableBytes < dataLength) {
      buffer.resetReaderIndex
      return null
    }
    // get the data.
    //reset readerIndex to 0 and +5 to dataLength because we want the protocol and size
    //included in the message contents passed to the serializer. i.e.
    //protocol version is 1 byte, message size is 4 bytes = + 5 bytes below
    buffer.resetReaderIndex()
    val messageContents: Array[Byte] = new Array[Byte](dataLength+5)
    buffer.readBytes(messageContents)
    messageContents
  }
}