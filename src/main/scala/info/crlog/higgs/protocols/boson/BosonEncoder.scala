package info.crlog.higgs.protocols.boson

import io.netty.handler.codec.MessageToByteEncoder
import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.ByteBuf

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonEncoder extends MessageToByteEncoder[Array[Byte]] {
  def encode(ctx: ChannelHandlerContext, msg: Array[Byte], out: ByteBuf) {
    //protocol version, message size and message are already in the byte array so just write it out
    out.writeBytes(msg)
  }
}
