package info.crlog.higgs.protocol.boson

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{ChannelHandlerContext, Channel}
import info.crlog.higgs.protocol.HiggsDecoder


/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class BosonDecoder extends HiggsDecoder {
  override protected def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    // Wait until the length,flag and topic size prefix is available.
    if (buffer.readableBytes < 7) {
      return null
    }
    //set reader index to 0
    buffer.markReaderIndex()
    val dataLength: Int = buffer.readInt //get the data size, i.e. first 4 bytes
    val flag: Byte = buffer.readByte //get the flag, i.e. 5th byte
    val topicLength: Short = buffer.readShort //get the length of the topic from the current read index
    //wait until we get the entire topic
    if (buffer.readableBytes < topicLength) {
      buffer.resetReaderIndex()
      return null
    }
    val topicContents: Array[Byte] = new Array[Byte](topicLength)
    buffer.readBytes(topicLength).readBytes(topicContents)

    // Wait until all the full message is available
    if (buffer.readableBytes < dataLength) {
      buffer.resetReaderIndex()
      return null
    }
    // Convert the received data into a new BosonMessage.
    val messageContents: Array[Byte] = new Array[Byte](dataLength)
    buffer.readBytes(messageContents)

    new BosonMessage(flag,topicContents, messageContents)
  }
}