package info.crlog.higgs.agents.omsg

import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */

class OMsgDecoder extends ByteToMessageDecoder[Array[Byte]] {

  def decode(ctx: ChannelHandlerContext, buffer: ByteBuf): Array[Byte] = {
    // Wait until the size of the message is available.
    if (buffer.readableBytes < 4) {
      return null
    }
    //set reader index to 0
    buffer.markReaderIndex
    val dataLength: Int = buffer.readInt //get the data size, i.e. first 4 bytes (32 bit signed java int)
    // Wait until all the full message is available
    if (buffer.readableBytes < dataLength) {
      buffer.resetReaderIndex
      return null
    }
    // get the data.
    val messageContents: Array[Byte] = new Array[Byte](dataLength)
    buffer.readBytes(messageContents)
    messageContents
  }
}

