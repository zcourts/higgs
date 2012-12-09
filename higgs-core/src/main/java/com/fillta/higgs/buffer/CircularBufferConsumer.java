package com.fillta.higgs.buffer;

import com.fillta.higgs.util.Function1;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class CircularBufferConsumer<T> extends RecursiveAction {
	protected CircularBuffer<T> buffer = new CircularBuffer();
	protected ForkJoinPool threadPool;
	protected Function1<T> function;
	/**
	 * This is the consumer's sequence which indicates where this consumer
	 * has gotten up to in reading from the buffer.
	 * It is independently read and updated (to reduce contention) from the Buffer's sequence, see the
	 * link on the {@link CircularBuffer}'s class for more info.
	 */
	protected AtomicInteger sequence = new AtomicInteger(-1);
	protected int threshold;
	private int toSequence;

	public CircularBufferConsumer(ForkJoinPool threadPool, Function1<T> function,
	                              CircularBuffer<T> buffer, int threshold, int sequence, int toSequence) {
		this.buffer = buffer;
		this.threadPool = threadPool;
		this.function = function;
		this.threshold = threshold;
		this.sequence.lazySet(sequence);
		this.toSequence = toSequence;
	}

	@Override
	protected void compute() {
		int index = sequence.incrementAndGet();
		while (index <= buffer.sequence()) {
			//if the buffer's sequence - consumer sequence > threshold then
			//there are too many messages for this consumer alone, split into two consumers,repeatedly if need be
			if ((buffer.sequence() - sequence.get()) > threshold) {
				System.out.println("Splitting into 2 circular buffer consumers");
				//split and invoke
				int split = (int) (buffer.sequence() - sequence.get()) / 2;
				invokeAll(
						new CircularBufferConsumer<>(threadPool, function, buffer, threshold, index, split),
						new CircularBufferConsumer<>(threadPool, function, buffer, threshold, split + 1, (int) buffer.sequence())
				);
				break;
			} else {
				//should be enough for one consumer
				T type = buffer.get(index);
				function.call(type);
			}
			//re-evaluate on every iteration
			index = sequence.incrementAndGet();
			index = index % buffer.size();
			sequence.lazySet(index);
		}
		//toSequence = (int) buffer.sequence();
		reinitialize();
	}

}
