package com.fillta.higgs.events;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ChannelMessage<M> {
    public Channel channel;
    public final M message;
    public final ChannelHandlerContext context;
    /**
     * A message is out going, i.e. being sent to a server.
     * If it does not have a context.
     */
    public final boolean isOutGoing;
    //convenience
    public final boolean isIncoming;

    public ChannelMessage(final ChannelHandlerContext ctx, final M m) {
        context = ctx;
        if (ctx != null) {
            channel = ctx.channel();
            isOutGoing = false;
        } else {
            isOutGoing = true;
        }
        isIncoming = !isOutGoing;
        message = m;
    }
}
