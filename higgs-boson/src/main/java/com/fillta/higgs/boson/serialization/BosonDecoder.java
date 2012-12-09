package com.fillta.higgs.boson.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonDecoder extends ByteToMessageDecoder<ByteBuf> {
    @Override
    public ByteBuf decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        // Wait until the protocol version and size of the message is available.
        if (buffer.readableBytes() < 5) {
            return null;
        }
        //mark reader index at 0 so we can reset to it later.
        buffer.markReaderIndex();
        int protocolVersion = buffer.readByte();
        int dataLength = buffer.readInt(); //get the data size, i.e. 4 bytes (32 bit signed java int)
        // Wait until the full message is available
        if (buffer.readableBytes() < dataLength) {
            buffer.resetReaderIndex();
            return null;
        }
        //from 0th byte to dataLength + 5 (1 byte = protocol, 4 bytes = size hence + 5)
        int readIndex = dataLength + 5;
        buffer.resetReaderIndex();
        ByteBuf b = Unpooled.buffer(readIndex);
        buffer.readBytes(b, readIndex);
        return b;
    }
}
