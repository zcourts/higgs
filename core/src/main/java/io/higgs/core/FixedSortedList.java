package io.higgs.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * A list which sorts the items it is created with.
 * Sort order is not maintained.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FixedSortedList<T> extends ArrayList<T> implements Comparator<Sortable<T>> {
    /**
     * Create a new list and sort the provided parameters
     *
     * @param set the collection to sort
     */
    public FixedSortedList(Collection<T> set) {
        super(set);
        Collections.sort(this, (Comparator<? super T>) this);
    }

    @Override
    public int compare(Sortable<T> a, Sortable<T> b) {
        return a.priority() > b.priority() ? -1 : (a.priority() == b.priority() ? 0 : 1);
    }
}
