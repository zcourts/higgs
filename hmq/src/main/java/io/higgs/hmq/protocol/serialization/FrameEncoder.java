package io.higgs.hmq.protocol.serialization;

import io.higgs.hmq.protocol.Frame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class FrameEncoder extends MessageToByteEncoder<Frame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame msg, ByteBuf out) throws Exception {
        out.writeBytes(msg.encode());
    }
}
