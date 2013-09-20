package io.higgs.hmq.protocol.serialization;

import io.higgs.hmq.protocol.Handshake;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HandshakeEncoder extends MessageToByteEncoder<Handshake> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Handshake msg, ByteBuf out) throws Exception {
        out.writeBytes(msg.data());
        //remove from pipeline, handshake is only done once
        ctx.pipeline().remove(this);
    }
}
