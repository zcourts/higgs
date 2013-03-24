package io.higgs.boson.serialization.kryo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class KryoDecoder extends ByteToMessageDecoder {
    @Override
    public ByteBuf decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        // Wait until the size of the message is available.
        if (buffer.readableBytes() < 4) {
            return null;
        }
        //mark reader index at 0 so we can reset to it later.
        buffer.markReaderIndex();
        int size = buffer.readInt(); //get the data size, i.e. 4 bytes (32 bit signed java int)
        // Wait until the full message is available
        if (buffer.readableBytes() < size) {
            buffer.resetReaderIndex();
            return null;
        }
        ByteBuf b = ctx.alloc().buffer(size);
        buffer.readBytes(b, size);
        return b;
    }
}
