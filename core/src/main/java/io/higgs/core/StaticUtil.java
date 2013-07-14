package io.higgs.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class StaticUtil {
    public static ChannelFuture write(Channel channel, Object o) {
        return channel.writeAndFlush(o);
    }

    public static ChannelFuture write(ChannelHandlerContext ctx, Object o) {
        return ctx.writeAndFlush(o);
    }
}
