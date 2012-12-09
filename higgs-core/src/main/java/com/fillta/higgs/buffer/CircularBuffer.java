package com.fillta.higgs.buffer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Inspired by <a href="http://www.youtube.com/watch?v=DCdGlxBbKU4">http://www.youtube.com/watch?v=DCdGlxBbKU4</a>
 * <pre>
 * Key points at times:
 * ~20:00 - ~23:30
 * ~26:20 - ~29
 * ~34  - ~3
 * </pre>
 * see the <a href="https://github.com/LMAX-Exchange/disruptor">Disruptor's</a> page.
 * Though nowhere near as sophisticated or optimised it gets the job done without locking.
 * Considered using the disruptor itself but has proven to be an overkill for this.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class CircularBuffer<T> {
	private final int SIZE;
	private final T[] data;
	private AtomicLong sequence = new AtomicLong(-1);
	private AtomicLong nextValue = new AtomicLong(-1);

	public CircularBuffer() {
		//1M empty array = about 22MB memory
		this(1000000);
	}

	public CircularBuffer(int size) {
		SIZE = size;
		data = (T[]) new Object[SIZE];
	}

	/**
	 * Adds an item to this buffer. If adding the item will cause the buffer to exceed
	 * its size the buffer's sequence is reset to 0 and the item at data[0] will be overwritten
	 * and every addition thereafter will cause data[N] to be overwritten.
	 * This is repeated indefinitely. e.g.
	 * In the below code, Items will be added at data[0] to data[size+2]
	 * Size is 10 so 12 items will be added. The last two items will overwrite
	 * data[0] and data[1] because size+2 exceeds the buffer's size by 2.
	 * <pre>
	 * {@code
	 * int size = 10;
	 * int limit = size;
	 * CircularBuffer buffer = new CircularBuffer(size);
	 * for (int i = 0; i < size + 2; i++) {
	 *      buffer.publish(i);
	 * }
	 * for (int i = 0; i < limit; i++) {
	 *      System.out.println("RETURNED:" + buffer.get(i));
	 * }
	 * }
	 * </pre>
	 *
	 * @param value The value to publish/add
	 */
	public void add(T value) {
		long index = nextValue.incrementAndGet();
		data[(int) index % SIZE] = value;
		sequence.lazySet(index);
	}

	public T get(int index) {
		if (index <= sequence()) {
			return data[index % SIZE];
		}
		return null;
	}

	/**
	 * Sets the item at data[index] = null, i.e. if you have a memory hogging object
	 * being referenced that you <em>KNOW</em> you no longer need.
	 *
	 * @param index The index of the item.
	 * @return true if  -1 < index < data.length
	 */
	public boolean evict(int index) {
		if (index <= data.length && index > -1) {
			data[index] = null;
			return true;
		}
		return false;
	}

	public long sequence() {
		return sequence.get();
	}

	public int size() {
		return SIZE;
	}
}
