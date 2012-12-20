package com.fillta.higgs.buffer;

import com.fillta.functional.Function1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class CircularBufferConsumer<T> {
	protected ExecutorService threadPool;
	protected Function1<T> function;
	private LinkedBlockingQueue<Integer> sync = new LinkedBlockingQueue<>();
	protected AtomicReference<Boolean> running = new AtomicReference<>(false);
	/**
	 * This is the consumer's sequence which indicates where this consumer
	 * has gotten up to in reading from the buffer.
	 * It is independently read and updated (to reduce contention) from the Buffer's sequence, see the
	 * link on the {@link CircularBuffer}'s class for more info.
	 */
	protected Sequence sequence;
	protected int threshold;
	private CircularBuffer<T> buffer;

	public CircularBufferConsumer(ExecutorService threadPool, Function1<T> function, CircularBuffer<T> buffer) {
		this.threadPool = threadPool;
		this.function = function;
		this.buffer = buffer;
		this.sequence = new Sequence(buffer);
	}

	public void updated() {
		sync.add(1);
	}

	public void shutdown() {
		running.lazySet(false);
		sync.add(1);
	}

	public void start() {
		running.set(true);
		threadPool.submit(new Runnable() {
			public void run() {
				while (running.get()) {
					try {
						//TODO revise logic, there are cases where sync will unblock and
						// the same task could potentially be processed multiple times.
						//could keep atomic reference to the last index or the last task and compare before looping
						while (buffer.sequence() > sequence.get()) {
							int index = sequence.index();
							T task = buffer.get(index);
							//automatic clean up, prevent unnecessary memory use
							buffer.evict(index);
							processTask(task);
						}
						//block the reading thread until the consumer is notified that something has been added
						sync.take();
					} catch (Throwable e) {
						//catch everything here to avoid exiting the loop on error
						//this can happen several hundreds or even thousands of times per second
						//need an alternative way to report it, have seen situation where errors swamped
						//logging framework and ground everything to a halt but these errors need
						// to be reported somewhow
					}
				}
			}

			private void processTask(final T task) {
				threadPool.submit(new Runnable() {
					public void run() {
						function.apply(task);
					}
				});
			}
		});
	}
//	public  void start() {
//		int index = sequence.incrementAndGet();
//		while (index <= buffer.sequence()) {
//			//if the buffer's sequence - consumer sequence > threshold then
//			//there are too many messages for this consumer alone, split into two consumers,repeatedly if need be
//			if ((buffer.sequence() - sequence.get()) > threshold) {
//				System.out.println("Splitting into 2 circular buffer consumers");
//				//split and invoke
//				int split = (int) (buffer.sequence() - sequence.get()) / 2;
//				invokeAll(
//						new CircularBufferConsumer<>(threadPool, function, buffer, threshold, index, split),
//						new CircularBufferConsumer<>(threadPool, function, buffer, threshold, split + 1, (int) buffer.sequence())
//				);
//				break;
//			} else {
//				//should be enough for one consumer
//				T type = buffer.get(index);
//				function.apply(type);
//			}
//			//re-evaluate on every iteration
//			index = sequence.incrementAndGet();
//			index = index % buffer.size();
//			sequence.lazySet(index);
//		}
//		//toSequence = (int) buffer.sequence();
//		reinitialize();
//	}

}
