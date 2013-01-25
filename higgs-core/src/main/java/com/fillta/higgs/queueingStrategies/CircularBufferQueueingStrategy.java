package com.fillta.higgs.queueingStrategies;

import com.fillta.functional.Function1;
import com.fillta.functional.Tuple;
import com.fillta.higgs.DecodedMessage;
import com.fillta.higgs.buffer.CircularBuffer;
import com.fillta.higgs.buffer.CircularBufferConsumer;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class CircularBufferQueueingStrategy<T, IM> extends QueueingStrategy<T, IM> {
	//use default buffer size of 1M
	protected CircularBuffer<Tuple<ChannelHandlerContext, DecodedMessage<T, IM>>> buffer = new CircularBuffer<>();
	protected ExecutorService threadPool;
	CircularBufferConsumer<Tuple<ChannelHandlerContext, DecodedMessage<T, IM>>> consumer;

	public CircularBufferQueueingStrategy(QueueingStrategy<T, IM> strategy, ExecutorService threadPool) {
		super(strategy);
		this.threadPool = threadPool;
		consumer = new CircularBufferConsumer<>(this.threadPool,
				new Function1<Tuple<ChannelHandlerContext, DecodedMessage<T, IM>>>() {
					public void apply(Tuple<ChannelHandlerContext, DecodedMessage<T, IM>> a) {
						if (a != null) {
							invokeListeners(a.key, a.value);
						}
					}
				}, buffer);
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
	public void enqueue(ChannelHandlerContext ctx, DecodedMessage<T, IM> msg) {
		buffer.add(new Tuple<>(ctx, msg));
		consumer.updated();
	}
}
