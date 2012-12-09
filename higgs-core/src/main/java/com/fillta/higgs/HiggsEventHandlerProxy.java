package com.fillta.higgs;

import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

import static com.fillta.higgs.events.HiggsEvent.*;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsEventHandlerProxy<T, OM, IM, SM> extends ChannelInboundMessageHandlerAdapter<SM> {
	private final EventProcessor<T, OM, IM, SM> events;
	private final Optional<Throwable> none = Optional.absent();

	public HiggsEventHandlerProxy(EventProcessor<T, OM, IM, SM> eventProcessor) {
		events = eventProcessor;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, SM msg) throws Exception {
		events.emit(MESSAGE_RECEIVED, ctx, none);
		events.emitMessage(ctx, msg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		events.emit(EXCEPTION_CAUGHT, ctx, Optional.of(cause));
	}


	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		events.emit(CHANNEL_REGISTERED, ctx, none);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		events.emit(CHANNEL_UNREGISTERED, ctx, none);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		events.emit(CHANNEL_ACTIVE, ctx, none);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		events.emit(CHANNEL_INACTIVE, ctx, none);
	}
}
