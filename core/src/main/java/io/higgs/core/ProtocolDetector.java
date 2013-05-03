package io.higgs.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ProtocolDetector {
    /**
     * Using the data available in {@param #in} check if the data matches the protocol this detector represents
     *
     * @param ctx the channel context
     * @param in  data available
     * @return true if the protocol matches, false otherwise
     */
    boolean detected(ChannelHandlerContext ctx, ByteBuf in);

    /**
     * Set up the pipeline adding a  {@link io.netty.handler.codec.ByteToMessageDecoder } and
     * a {@link io.netty.handler.codec.MessageToByteEncoder} as necessary
     *
     * @param p   the pipeline to add handlers to
     * @param ctx the context for the channel
     */
    MessageHandler<?, ?> setupPipeline(ChannelPipeline p, ChannelHandlerContext ctx);
}
