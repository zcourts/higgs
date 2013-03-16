package io.higgs.events.listeners;

import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;

/**
 * Generic events that do not have an exception associated with them
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class ChannelEvent extends ChannelEventListener {
    public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
        triggered(ctx);
    }

    public abstract void triggered(ChannelHandlerContext ctx);
}
