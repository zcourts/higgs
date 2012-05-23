package info.crlog.higgs.messaging;

import io.netty.buffer.ChannelBuffer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.oneone.OneToOneDecoder;

/**
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsDecoder extends OneToOneDecoder {

    protected MessageFactory factory;

    public HiggsDecoder(MessageFactory factory) {
        this.factory = factory;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object channelBufferMessage) throws Exception {
        if (!(channelBufferMessage instanceof ChannelBuffer)) {
            return channelBufferMessage;
        }
        ChannelBuffer buffer = ((ChannelBuffer) channelBufferMessage);
        ReUsableMessage msg = factory.newMessage();
        int size = msg.getMessageSize(buffer);
        if (size == -1) {
            buffer.resetReaderIndex();
            return null;
        }
        if (buffer.readableBytes() < size) {
            buffer.resetReaderIndex();
            return null;
        }
        buffer.resetReaderIndex();
        //if we get here we've read the entire message
        //the message can populate its internal fields and be returned
        msg.deserialize(buffer);
        return msg;
    }
}
