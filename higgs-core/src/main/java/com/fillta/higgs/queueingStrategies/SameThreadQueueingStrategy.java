package com.fillta.higgs.queueingStrategies;

import com.fillta.higgs.MessageTopicFactory;
import com.fillta.functional.Function1;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class SameThreadQueueingStrategy<T, IM> extends QueueingStrategy<T, IM> {
	public SameThreadQueueingStrategy(MessageTopicFactory<T, IM> topicFactory) {
		super(topicFactory);
	}

	/**
	 * Invokes all {@link Function1}s subscribed to this message's topic on the current thread
	 * without buffering/queueing
	 *
	 * @param ctx the channel context
	 * @param msg the message to queue
	 */
	@Override
	public void enqueue(ChannelHandlerContext ctx, IM msg) {
		invokeListeners(ctx, msg);
	}
}
