package info.crlog.higgs.protocol.boson

import org.jboss.netty.handler.codec.frame.{CorruptedFrameException, FrameDecoder}
import java.math.BigInteger
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{ChannelHandlerContext, Channel}


/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class BosonDecoder extends FrameDecoder {
  override protected def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    // Wait until the length prefix is available.
    if (buffer.readableBytes < 5) {
      return null
    }
    buffer.markReaderIndex
    // Wait until the whole data is available.
    val dataLength: Int = buffer.readInt
    if (buffer.readableBytes < dataLength) {
      buffer.resetReaderIndex
      return null
    }
    // Convert the received data into a new BosonMessage.
    val decoded: Array[Byte] = new Array[Byte](dataLength)
    buffer.readBytes(decoded)
    return new BosonMessage(decoded)
  }
}