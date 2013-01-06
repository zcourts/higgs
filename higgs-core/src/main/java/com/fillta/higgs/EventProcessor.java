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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.logging.InternalLoggerFactory;
import io.netty.logging.Slf4JLoggerFactory;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
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
	static {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	protected Logger log = LoggerFactory.getLogger(getClass());
	private static final AttributeKey<Object> REQUEST_KEY = new AttributeKey<>("request-key");
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
	protected final Set<HiggsInterceptor> interceptors =
			Collections.newSetFromMap(new ConcurrentHashMap<HiggsInterceptor, Boolean>());

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
		//always associate the request with the channel
		ctx.channel().attr(REQUEST_KEY).set(msg);
		boolean intercepted = false;
		for (HiggsInterceptor interceptor : interceptors) {
			if (interceptor.matches(msg)) {
				intercepted = interceptor.intercept(ctx, msg);
				if (intercepted)
					break;//don't attempt to process the message, it has been intercepted!
			}
		}
		if (!intercepted) {
			IM imsg = deserialize(ctx, msg);
			//if de-serializer returns null then do not queue
			if (imsg != null) {
				messageQueue.enqueue(ctx, imsg);
			}
		}
	}

	/**
	 * When an exception occurs it is not always possible to know which request caused it.
	 * The only thing that is always accessible is the channel, to get access to the request
	 * we can associate data with each channel. In this case they data associated is the request
	 * object. To get the request you need to something similar to:
	 * <pre>
	 * {@code
	 * EventProcessor.on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
	 * 		public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
	 * 			Attribute<Object> request = ctx.channel().attr(EventProcessor.REQUEST_KEY);
	 * 		    if(request.get()!=null && request instanceof MyRequestType){
	 * 		        //do something clever
	 * 		    }
	 *         }
	 *     })
	 * }
	 * </pre>
	 * The {@link EventProcessor} guarantees that it will always set the request object.
	 * You must always check the type of the object returned.
	 * A {@link HiggsInterceptor} might add an unexpected type which would result in a ClassCastException. For
	 * e.g. The HS3 implementation expects an {@link HttpRequest} but the WebSocket server uses an interceptor
	 * and adds {@link TextWebSocketFrame}...clearly not compatible. Always check the request is of the
	 * expected type before casting.
	 */
	public Object getRequest(Channel channel) {
		if (channel == null)
			return null;//have it back!
		Attribute<Object> request = channel.attr(REQUEST_KEY);
		return request;
	}

	/**
	 * Registers an interceptor to this even processor
	 *
	 * @param interceptor the interceptor to add
	 * @param <T>
	 */
	public <T extends HiggsInterceptor> void addInterceptor(T interceptor) {
		if (interceptor == null)
			throw new NullPointerException("Cannot add a null interceptor");
		interceptors.add(interceptor);
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
	 *         use .addListener(ChannelFutureListener.CLOSE) to close the connection.
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
