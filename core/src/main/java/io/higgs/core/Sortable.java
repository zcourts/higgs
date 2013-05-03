package io.higgs.core;

/**
 * Sortable objects are used in {@link java.util.TreeSet}s.
 * The larger the priority the closer the object is to the front of the queue
 */
public interface Sortable<T> extends Comparable<T> {
    int priority();
}
