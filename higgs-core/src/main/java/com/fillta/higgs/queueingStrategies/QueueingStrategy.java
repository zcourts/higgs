package com.fillta.higgs.queueingStrategies;

import com.fillta.functional.Function1;
import com.fillta.higgs.DecodedMessage;
import com.fillta.higgs.events.ChannelMessage;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This package exists largely because of an observation in the Scala implementation.
 * When messages are received or sent they need to be off loaded and processed.
 * If processing 1 message takes 1 second or even 1 millisecond when there are hundreds of thousands of messages to
 * be processed, this can take time and cause a backlog.
 * Expirments with queues of all types showed that even if messages are "buffered"/queued and processed with multiple
 * threads, the queue then becomes the bottle neck. In some observations various {@link java.util.Queue} implementations
 * spend more time locking than the threads do processing, i.e. the contention rate sky rockets!
 * <p/>
 * Different approaches yield different results, performance and "safety" wise.
 * <p/>
 * By default {@link com.fillta.higgs.EventProcessor} uses the {@link SameThreadQueueingStrategy} which processes
 * messages on the same thread the arrived on. This is as safe is it gets but has the obvious drawback that if while
 * processing one of those messages a thread gets blocked, you're blocking the Netty worker thread, blocking all of
 * them could render the {@link com.fillta.higgs.EventProcessor} useless.
 * <p/>
 * The {@link LinkedBlockingQueueStrategy} uses Java's {@link java.util.concurrent.ConcurrentLinkedQueue} to buffer
 * messages, it then uses the {@link java.util.concurrent.ForkJoinPool} and {@link java.util.concurrent.RecursiveAction}
 * to process messages from the queue. This gets rid of the chances of you blocking the Netty threads but introduces
 * a high contention ratio with multiple {@link java.util.concurrent.RecursiveAction}s reading from the queue.
 * <p/>
 * {@link CircularBufferQueueingStrategy} uses our {@link com.fillta.higgs.buffer.CircularBuffer} implementation.
 * See <a href="http://en.wikipedia.org/wiki/Circular_buffer">http://en.wikipedia.org/wiki/Circular_buffer</a>
 * for an overview and <a href="http://www.youtube.com/watch?v=DCdGlxBbKU4">http://www.youtube.com/watch?v=DCdGlxBbKU4</a>
 * for the basis of our implementation.
 * <p/>
 * So, the biggest risk with using the circular buffer is the possibility of losing messages. A circular buffer by its nature
 * writes stuff into its backing array until it is full. Once full, it starts overwriting the oldest items in the buffer.
 * By default {@link com.fillta.higgs.buffer.CircularBuffer} initializes its backing array to 1 million. This should
 * be enough for most cases and results in about 20MB of memory being used just for the empty array;
 * <p/>
 * The advantage, the {@link java.util.concurrent.ForkJoinPool} can be used with a {@link java.util.concurrent.RecursiveAction}
 * with little to no read contention by multiple processing threads. This significantly speeds up message processing
 * as the buffer does not lock, so the processing threads do not contend when fetching items from the buffer.
 * If memory is not an issue the circular buffer can easily have its backing array size increased. However, if an application
 * is having 1M messages queued in memory chances are increasing the array size won't do much good since you're just going
 * to hit that larger size anyway, optimization is probably needed in the application itself. Or simply increasing
 * the number of message processing threads. Not too much, or it'll just lead to too much context switching, that
 * in itself being counter productive and becoming somewhat of a bottleneck.
 * <p/>
 * <p/>
 * A queueing strategy allows different {@link com.fillta.higgs.EventProcessor}s to handle messages in different ways.
 * For example, the {@link CircularBufferQueueingStrategy} pops messages in a
 * {@link com.fillta.higgs.buffer.CircularBuffer} - This allows 1 thread to write to the buffer
 * while multiple threads consume items from the buffer without contention.
 * The {@link LinkedBlockingQueueStrategy} uses a {@link java.util.concurrent.ConcurrentLinkedQueue}
 * which is safer but increases contention amongst threads reading and/or writing to it.
 * The {@link SameThreadQueueingStrategy} invokes listeners on the calling thread, i.e. the Netty
 * threads. This works, it is simple but if listeners block it can cause issues.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class QueueingStrategy<T, IM> {
	protected final ConcurrentHashMap<T, Set<Function1<ChannelMessage<IM>>>> messageSubscribers = new ConcurrentHashMap<>();
	protected final Set<Function1<ChannelMessage<IM>>> allMessageSubscribers = Collections.newSetFromMap(new ConcurrentHashMap<Function1<ChannelMessage<IM>>, Boolean>());
	protected Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Invoked when a message is received.
	 * Implementations are expected to take the message,
	 * 1) queue it if necessary,
	 * 2) get the topic from the message
	 * 3) invoke the message subscribers for the topic that are registered in the
	 * associated {@link com.fillta.higgs.EventProcessor}.
	 *
	 * @param ctx the channel context
	 * @param msg the message to queue
	 */
	public abstract void enqueue(ChannelHandlerContext ctx, DecodedMessage<T, IM> msg);

	/**
	 * Subscribes a function/callback to the given topic
	 *
	 * @param topic    the topic to listen to
	 * @param function the callback to be invoked
	 */
	public void listen(T topic, Function1<ChannelMessage<IM>> function) {
		Set<Function1<ChannelMessage<IM>>> set = messageSubscribers.get(topic);
		if (set == null) {
			//set must be backed by concurrent hash map to be thread safe
			set = Collections.newSetFromMap(new ConcurrentHashMap<Function1<ChannelMessage<IM>>, Boolean>());
			messageSubscribers.put(topic, set);
		}
		set.add(function);
	}

	/**
	 * Subscribes the given function to <em>all</em> messages/events.
	 *
	 * @param function
	 */
	public void listen(Function1<ChannelMessage<IM>> function) {
		allMessageSubscribers.add(function);
	}

	/**
	 * @param topic the topic to check
	 * @return true if at least one function is subscribed to the given topic
	 */
	public boolean listening(T topic) {
		synchronized (messageSubscribers) {
			return messageSubscribers.containsKey(topic);
		}
	}

	/**
	 * Invoke all listeners to the given message on the current thread.
	 *
	 * @param ctx
	 * @param msg
	 */
	public void invokeListeners(ChannelHandlerContext ctx, DecodedMessage<T, IM> msg) {
		T topic = msg.getTopic();
		ChannelMessage<IM> a = new ChannelMessage<>(ctx, msg.getMessage());
		//functions subscribed to "all" messages (note: new ArrayList(collection) copies)
		List<Function1<ChannelMessage<IM>>> listeners = new ArrayList<>(allMessageSubscribers);
		for (Function1<ChannelMessage<IM>> function : listeners) {
			function.apply(a);
		}
		//now process functions subscribed only to this message's topic
		Set<Function1<ChannelMessage<IM>>> set = messageSubscribers.get(topic);
		if (set == null) {
			listeners = new ArrayList<>();
		} else {
			listeners = new ArrayList<>(set);
		}
		for (Function1<ChannelMessage<IM>> function : listeners) {
			function.apply(a);
		}
		//only incoming messages count
		if (listeners.size() == 0 && !a.isOutGoing) {
			log.warn(String.format("Message received and decoded but no listeners found. Topic:%s", topic));
		}
	}

	/**
	 * @param topic Removes all functions subscribed to the given topic
	 */
	public void removeAll(T topic) {
		Set<Function1<ChannelMessage<IM>>> set = messageSubscribers.get(topic);
		if (set != null) {
			set.clear();
		}
	}

	/**
	 * Un-subscribe the given function under the given topic
	 *
	 * @param topic    the topic the function is subscribed to
	 * @param function the function to be removed
	 */
	public void remove(T topic, Function1<ChannelMessage<IM>> function) {
		synchronized (messageSubscribers) {
			Set<Function1<ChannelMessage<IM>>> listeners = messageSubscribers.get(topic);
			if (listeners != null) {
				listeners.remove(function);
			}
		}
	}
}