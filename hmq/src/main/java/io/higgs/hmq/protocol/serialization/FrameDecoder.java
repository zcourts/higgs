package io.higgs.hmq.protocol.serialization;

import io.higgs.hmq.protocol.IllegalFrameSizeException;
import io.higgs.hmq.protocol.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static io.higgs.hmq.ByteUtil.isBitSet;

public class FrameDecoder extends ByteToMessageDecoder {
    private boolean moreFramesToCome;
    private byte[] topic;
    private ByteBuf contents = Unpooled.buffer();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 1) {
            return;
        }
        in.markReaderIndex();
        //get the first readable byte, it's the flag
        //if the lsb i.e. bit 0 == 1 then more frames to follow, otherwise this is the only frame
        byte flag = in.readByte();
        moreFramesToCome = isBitSet(flag, 0);
        //if bit 1 is set then it's a long message
        boolean longMessage = isBitSet(flag, 1);
        long size;
        if (longMessage) {
            if (in.readableBytes() < 8) {
                in.resetReaderIndex();
                return;
            }
            size = in.readLong();
        } else {
            if (in.readableBytes() < 1) {
                in.resetReaderIndex();
                return;
            }
            size = in.readUnsignedByte();
        }
        if (size > Integer.MAX_VALUE) {
            throw new IllegalFrameSizeException("A frame size of %s bytes was received, this is too big.");
        }
        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[(int) size];
        in.readBytes(data);
        //topic is always the first part of a message
        //since the max body of a subscription is 255 bytes the topic will always be in the first frame
        if (topic == null) {
            topic = data;
        } else {
            contents.writeBytes(data);
        }
        //if there are no more frames to come we have the entire message
        if (moreFramesToCome == false) {
            out.add(new Message(topic, contents));
            topic = null;
            contents = Unpooled.buffer();
        }
    }
}
