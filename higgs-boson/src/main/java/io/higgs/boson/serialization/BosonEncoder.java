package io.higgs.boson.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.LoggerFactory;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonEncoder extends MessageToByteEncoder<ByteBuf> {
    @Override
    public void encode(final ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        out.writeBytes(msg);
        ctx.flush().addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                //if flushing failed then we have no choice but to close the connection
                //data may have been sent and the client will append the next message to the partial
                //data it received before, leading to corruption of all future messages
                if (!future.isSuccess()) {
                    LoggerFactory.getLogger(getClass()).warn("Failed to flush message", future.cause());
                    ctx.close();
                }
            }
        });
    }
}
