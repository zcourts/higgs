package io.higgs.hmq.protocol.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;

import static io.higgs.hmq.ByteUtil.isBitSet;

public class FrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 1) {
            return;
        }
        in.markReaderIndex();
        //get the first readable byte, it's the flag
        //if the lsb i.e. bit 0 == 1 then more frames to follow, otherwise this is the only frame
        byte flag = in.readByte();
        boolean moreFramesToCome = isBitSet(flag, 0);
        //if bit 1 is set then it's a long message
        boolean longMessage = isBitSet(flag, 1);
        long size;
        if (longMessage) {
            if (in.readableBytes() < 8) {
                in.resetReaderIndex();
                return;
            }
            byte[] b = new byte[8];
            in.readBytes(b);
            size = new BigInteger(b).longValue();
        } else {
            if (in.readableBytes() < 1) {
                in.resetReaderIndex();
                return;
            }
            size = in.readByte();
        }
        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return;
        }
        ByteBuf body = ctx.alloc().buffer();
        in.readBytes(body, (int) size);
        System.out.println(body.toString(Charset.forName("UTF-8")));
    }
}
