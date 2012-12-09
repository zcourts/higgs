package com.fillta.higgs;

import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.queueingStrategies.CircularBufferQueueingStrategy;
import com.fillta.higgs.queueingStrategies.LinkedBlockingQueueStrategy;
import com.fillta.higgs.queueingStrategies.QueueingStrategy;
import com.fillta.higgs.queueingStrategies.SameThreadQueueingStrategy;
import com.fillta.higgs.util.Function1;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;

import static com.fillta.higgs.events.HiggsEvent.EXCEPTION_CAUGHT;

/**
 * @param <T>  Type of the topic this event processor is for
 * @param <OM> The outgoing message type e.g. HTTPRequest
 * @param <IM> The incoming message type e.g. HTTPResponse
 * @param <SM> The serialized form of both messages, typically {@code byte[]} or more efficiently
 *             {@link io.netty.buffer.ByteBuf} since byte[] gets converted to it anyway...
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class EventProcessor<T, OM, IM, SM> {
	protected Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * One of the worse errors/bugs to have is a thread terminating because of an uncaught exception.
	 * It leaves no indication of what happened and sometimes the error happens on a thread you don't
	 * have control over, which means you cannot directly catch the exception.
	 * This field is an alternative to just letting it die. It catches uncaught exceptions
	 * and triggers a normal {@link com.fillta.higgs.events.HiggsEvent}.EXCEPTION_CAUGHT.
	 * As such all exceptions can be caught and handled (logged etc) making it easier to debug issues
	 * related to this.
	 */
	protected Thread.UncaughtExceptionHandler unhandledExceptionHandler = new Thread.UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			emit(EXCEPTION_CAUGHT, null, Optional.of(e));
		}
	};
	protected int availableProcessors = Runtime.getRuntime().availableProcessors();
	protected int maxThreads = availableProcessors * 4;
	protected boolean asyncForkJoin = true;
	protected ForkJoinPool threadPool = new ForkJoinPool(maxThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory,
			unhandledExceptionHandler, asyncForkJoin);

	protected ArrayListMultimap<HiggsEvent, ChannelEventListener> eventSubscribers = ArrayListMultimap.create();
	protected QueueingStrategy<T, IM> messageQueue;

	public EventProcessor() {
		messageQueue = messageQueue(threadPool);
		Thread.setDefaultUncaughtExceptionHandler(unhandledExceptionHandler);
	}


	/**
	 * Provide a queueing strategy for the event processor.
	 * By default this uses {@link com.fillta.higgs.queueingStrategies.SameThreadQueueingStrategy} which will
	 * not use a queue and instead invoke all listeners immediately on the inbound messages
	 * thread.
	 *
	 * @param threadPool A configured thread pool for the message queue to use if required.
	 * @return
	 */
	protected QueueingStrategy<T, IM> messageQueue(ForkJoinPool threadPool) {
		return new SameThreadQueueingStrategy(topicFactory());
	}

	/**
	 * Sets the Queueing strategy used to process incoming messages.
	 * For a detailed explanation see the documentation of {@link QueueingStrategy}
	 *
	 * @param strategy the strategy to use.
	 */
	public void setQueueingStrategy(QueueingStrategy<T, IM> strategy) {
		messageQueue = strategy;
	}

	/**
	 * Set the queueing strategy to use a {@link com.fillta.higgs.buffer.CircularBuffer}
	 *
	 * @param threshold The max number of messages a single thread/consumer should try to
	 *                  process before splitting into multiple consumers
	 */
	public void setQueueingStrategyAsCircularBuffer(int threshold) {
		messageQueue = new CircularBufferQueueingStrategy<T, IM>(threadPool, topicFactory(), threshold);
	}

	/**
	 * Set the queueing strategy to use a {@link java.util.concurrent.LinkedBlockingQueue}
	 */
	public void setQueueingStrategyAsBlockingQueue() {
		messageQueue = new LinkedBlockingQueueStrategy<T, IM>(threadPool, topicFactory());
	}

	public void emit(HiggsEvent event, ChannelHandlerContext context, Optional<Throwable> ex) {
		for (ChannelEventListener l : eventSubscribers.get(event)) {
			l.triggered(context, ex);
		}
	}

	public void emitMessage(ChannelHandlerContext ctx, SM msg) {
		IM imsg = deserialize(ctx, msg);
		//if de-serializer returns null then do not queue
		if (imsg != null) {
			messageQueue.enqueue(ctx, imsg);
		}
	}

	/**
	 * De-Serialize a message
	 *
	 * @param msg the serialized message to be de-serialized
	 * @return
	 */
	public IM deserialize(ChannelHandlerContext ctx, SM msg) {
		return serializer().deserialize(ctx, msg);
	}

	/**
	 * Serialize a message
	 *
	 * @param msg the outgoing message to be serialized
	 * @return
	 */
	public SM serialize(Channel channel, OM msg) {
		return serializer().serialize(channel, msg);
	}

	public void on(HiggsEvent event, ChannelEventListener listener) {
		eventSubscribers.put(event, listener);
	}

	/**
	 * Subscribes the given function to the specified topic
	 *
	 * @param topic    The topic to listen for
	 * @param function The function to be invoked when a message for the given topic is received
	 */
	public void listen(T topic, final Function1<ChannelMessage<IM>> function) {
		messageQueue.listen(topic, function);
	}

	/**
	 * Subscribes the given function to <em>all</em> messages/events.
	 *
	 * @param function The function should return true if it uses the messages it receives
	 */
	public void listen(Function1<ChannelMessage<IM>> function) {
		messageQueue.listen(function);
	}

	public void unsubscribeAll(T topic) {
		messageQueue.removeAll(topic);
	}

	public boolean listening(T topic) {
		return messageQueue.listening(topic);
	}

	public void respond(Channel c, OM obj) {
		c.write(serializer().serialize(c, obj));
	}

	public abstract MessageConverter<IM, OM, SM> serializer();

	public abstract MessageTopicFactory<T, IM> topicFactory();
}
