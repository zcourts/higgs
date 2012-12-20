package com.fillta.higgs.queueingStrategies;

import com.fillta.functional.Function1;
import com.fillta.functional.Tuple;
import com.fillta.higgs.MessageTopicFactory;
import com.fillta.higgs.buffer.CircularBuffer;
import com.fillta.higgs.buffer.CircularBufferConsumer;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class CircularBufferQueueingStrategy<T, IM> extends QueueingStrategy<T, IM> {
	//use default buffer size of 1M
	protected CircularBuffer<Tuple<ChannelHandlerContext, IM>> buffer = new CircularBuffer<>();
	protected ExecutorService threadPool;
	CircularBufferConsumer<IM> consumer;

	public CircularBufferQueueingStrategy(ExecutorService threadPool,
	                                      MessageTopicFactory<T, IM> topicFactory) {
		super(topicFactory);
		this.threadPool = threadPool;
		consumer = new CircularBufferConsumer(this.threadPool,
				new Function1<Tuple<ChannelHandlerContext, IM>>() {
					public void apply(Tuple<ChannelHandlerContext, IM> a) {
						if (a != null) {
							invokeListeners(a.key, a.value);
						}
					}
				}, buffer);
		//so easy to forget to do this,but its probably best left as a separate operation
		consumer.start();
	}

	/**
	 * Queues the given message and submits the {@link #consumer} to the thread pool.
	 * The consumer maintains its state (sequence) so it knows where to start or continue reading
	 * from in the buffer.
	 *
	 * @param ctx the channel context
	 * @param msg the message to queue
	 */
	@Override
	public void enqueue(ChannelHandlerContext ctx, IM msg) {
		buffer.add(new Tuple(ctx, msg));
		//let consumer know the buffer has been updated
		consumer.updated();
	}
}
