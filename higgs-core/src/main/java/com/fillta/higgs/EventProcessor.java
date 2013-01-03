package com.fillta.higgs;

import com.fillta.functional.Function1;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.queueingStrategies.CircularBufferQueueingStrategy;
import com.fillta.higgs.queueingStrategies.LinkedBlockingQueueStrategy;
import com.fillta.higgs.queueingStrategies.QueueingStrategy;
import com.fillta.higgs.queueingStrategies.SameThreadQueueingStrategy;
import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
	public static final int availableProcessors = Runtime.getRuntime().availableProcessors();
	public static int maxThreads = availableProcessors;
	private static ThreadPoolExecutor threadPool;
	/**
	 * A set of event listeners that will be notified when a given event occurs.
	 * NOTE: used by multiple threads so the set's backing Map must be thread safe
	 */
	protected Map<HiggsEvent, Set<ChannelEventListener>> eventSubscribers = new ConcurrentHashMap<>();
	protected QueueingStrategy<T, IM> messageQueue;
	private AtomicBoolean daemonThreadPool = new AtomicBoolean();

	public EventProcessor() {
		messageQueue = messageQueue(threadPool);
		Thread.setDefaultUncaughtExceptionHandler(unhandledExceptionHandler);
	}

	/**
	 * @return A fixed size thread pool. where pool size = {@link #maxThreads}
	 */
	public ThreadPoolExecutor threadPool() {
		if (threadPool == null) {
			threadPool = new ThreadPoolExecutor(
					//minimum of 1 core thread
					Math.max(1, maxThreads / 2),
					//up to this many threads may be used
					maxThreads,
					30000L,
					TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
				private AtomicInteger totalThreads = new AtomicInteger();

				public Thread newThread(final Runnable r) {
					Thread thread = new Thread(r, getClass().getName() + "-higgs-" + totalThreads.getAndIncrement());
					thread.setDaemon(daemonThreadPool.get());
					thread.setDefaultUncaughtExceptionHandler(unhandledExceptionHandler);
					return thread;
				}
			});
			threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		}
		return threadPool;
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
	protected QueueingStrategy<T, IM> messageQueue(ExecutorService threadPool) {
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
	 */
	public void setQueueingStrategyAsCircularBuffer() {
		messageQueue = new CircularBufferQueueingStrategy<T, IM>(threadPool(), topicFactory());
	}

	/**
	 * Set the queueing strategy to use a {@link java.util.concurrent.LinkedBlockingQueue}
	 */
	public void setQueueingStrategyAsBlockingQueue() {
		messageQueue = new LinkedBlockingQueueStrategy<T, IM>(threadPool(), topicFactory());
	}

	public void emit(HiggsEvent event, ChannelHandlerContext context, Optional<Throwable> ex) {
		Set<ChannelEventListener> set = eventSubscribers.get(event);
		if (set != null) {
			for (ChannelEventListener l : set) {
				if (l != null) {
					l.triggered(context, ex);
				}
			}
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

		Set<ChannelEventListener> set = eventSubscribers.get(event);
		if (set == null) {
			//important: concurrent hash map used because events can be triggered from multiple threads
			//this means reading the set from multiple threads
			set = Collections.newSetFromMap(new ConcurrentHashMap<ChannelEventListener, Boolean>());
			eventSubscribers.put(event, set);
		}
		set.add(listener);
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
	 * Un-subscribe the given function under the given topic
	 *
	 * @param topic    the topic the function is subscribed to
	 * @param function the function to be removed
	 */
	public void unsubscribe(T topic, Function1<ChannelMessage<IM>> function) {
		messageQueue.remove(topic, function);
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

	/**
	 * @param c
	 * @param obj
	 * @return The write future. If you won't be writing any more and the connection won;t be needed
	 * use .addListener(ChannelFutureListener.CLOSE) to close the connection.
	 */
	public ChannelFuture respond(Channel c, OM obj) {
		return c.write(serializer().serialize(c, obj));
	}

	public abstract MessageConverter<IM, OM, SM> serializer();

	public abstract MessageTopicFactory<T, IM> topicFactory();

	public void setDaemonThreadPool(final boolean daemonThreadPool) {
		this.daemonThreadPool.set(daemonThreadPool);
	}
}
