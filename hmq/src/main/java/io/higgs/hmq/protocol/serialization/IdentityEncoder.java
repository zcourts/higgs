package io.higgs.hmq.protocol.serialization;

import io.higgs.hmq.protocol.Identity;
import io.higgs.hmq.protocol.SocketType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class IdentityEncoder extends MessageToByteEncoder<Identity> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Identity msg, ByteBuf out) throws Exception {
        out.writeByte(0x01); //revision
        out.writeByte(SocketType.SUB.getValue()); //socket type obviously
        out.writeByte(Identity.FINAL_BYTE); //final byte
    }
}
