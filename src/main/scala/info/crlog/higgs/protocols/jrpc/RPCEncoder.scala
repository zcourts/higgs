package info.crlog.higgs.protocols.jrpc

import io.netty.handler.codec.MessageToByteEncoder
import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.ByteBuf

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class RPCEncoder extends MessageToByteEncoder[Array[Byte]] {
  def encode(ctx: ChannelHandlerContext, msg: Array[Byte], out: ByteBuf) {
    out.writeInt(msg.length) // data length, i.e first 4 bytes ( an Int's 32 bits)
    out.writeBytes(msg) //now write the packed message's bytes
  }
}