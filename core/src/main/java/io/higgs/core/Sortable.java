package io.higgs.core;

/**
 * Sortable objects are used in {@link java.util.TreeSet}s.
 * The larger the priority the closer the object is to the front of the queue
 */
public interface Sortable<T> extends Comparable<T> {
    /**
     * Sets the priority of this sortable object
     *
     * @param value the new value
     * @return the old priority
     */
    int setPriority(int value);

    /**
     * Gets the current priority of this sortable object
     *
     * @return the current priority
     */
    int priority();
}
