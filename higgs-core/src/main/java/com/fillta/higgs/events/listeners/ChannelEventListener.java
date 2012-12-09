package com.fillta.higgs.events.listeners;

import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class ChannelEventListener {
	public abstract void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex);

	public boolean consume() {
		return false;
	}
}
