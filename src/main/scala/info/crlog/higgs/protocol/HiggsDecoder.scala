package info.crlog.higgs.protocol

import boson.BosonMessage
import io.netty.handler.codec.frame.FrameDecoder
import io.netty.channel.{Channel, ChannelHandlerContext}
import io.netty.buffer.ChannelBuffer

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */

class HiggsDecoder extends FrameDecoder {
  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    // Wait until the protocol version and message size is available
    if (buffer.readableBytes < 6) {
      return null
    }
    val protocol = buffer.readShort() //protocol version, first 2 bytes
    val messageSize = buffer.readInt() //message size, 3rd to 6th bytes, i.e. 32 bits
    //wait until we get the entire message
    if (buffer.readableBytes < messageSize) {
      buffer.resetReaderIndex()
      return null
    }
    if (protocol == Version.V1.version) {
      buffer.resetReaderIndex()
      return new BosonMessage(buffer.readBytes(messageSize))
    } else {
      throw new UnsupportedVersionException("Version, " + protocol + " is not currently supported")
    }
  }
}
