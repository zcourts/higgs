package com.fillta.higgs.buffer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Sequence {
    AtomicInteger sequence = new AtomicInteger(-1);
    private final CircularBuffer<?> buffer;

    public Sequence(CircularBuffer<?> buffer) {
        this.buffer = buffer;
    }

    public int inc() {
        return sequence.incrementAndGet();
    }

    public void set(int i) {
        sequence.lazySet(i);
    }

    public int get() {
        return sequence.get();
    }

    public int index() {
        int index = sequence.incrementAndGet();
        index = index % buffer.size();
        sequence.lazySet(index);
        return index;
    }

    public String toString() {
        return sequence.toString();
    }
}
