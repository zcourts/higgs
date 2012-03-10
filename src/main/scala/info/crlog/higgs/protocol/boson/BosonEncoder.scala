package info.crlog.higgs.protocol.boson

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.jboss.netty.channel.{ChannelHandlerContext, Channel}
import info.crlog.higgs.protocol.HiggsEncoder

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class BosonEncoder extends HiggsEncoder {
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
    val data: Array[Byte] = message.asBytes()
    val topic: Array[Byte] = message.topic.getBytes
    val dataLength: Int = data.length
    // Construct a message.
    val buf: ChannelBuffer = ChannelBuffers.dynamicBuffer
    buf.writeInt(dataLength) // data length, i.e first 4 bytes ( an Int's 32 bits)
    buf.writeByte(message.flag) //flag, i.e. 5th byte
    buf.writeShort(topic.length) //topic length,i.e 6th & 7th bytes
    buf.writeBytes(topic) //write the topic, 8th byte onwards
    buf.writeBytes(data) //data, the 9th + topic.length onwards
    // Return the constructed message.
    buf
  }
}