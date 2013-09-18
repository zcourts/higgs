package io.higgs.hmq.protocol.serialization;

import io.higgs.hmq.protocol.Signature;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class SignatureEncoder extends MessageToByteEncoder<Signature> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Signature msg, ByteBuf out) throws Exception {
        out.writeBytes(msg.rawBuffer());
    }
}
