package info.crlog.higgs.protocol.boson

import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.jboss.netty.channel.{ChannelHandlerContext, Channel}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class BosonEncoder extends OneToOneEncoder {
  protected def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): AnyRef = {
    // Convert to a BosonMessage
    var message: BosonMessage = null
    if (msg.isInstanceOf[BosonMessage]) {
      message = msg.asInstanceOf[BosonMessage]
    }
    else {
      message = new BosonMessage(msg)
    }
    // Convert the message to byte array.
    val data: Array[Byte] = message asBytes
    val dataLength: Int = data.length
    // Construct a message.
    val buf: ChannelBuffer = ChannelBuffers.dynamicBuffer
    buf.writeInt(dataLength) // data length
    buf.writeBytes(data) //data
    // Return the constructed message.
    buf
  }
}