package io.higgs.core;

import java.util.Comparator;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class SortableComparator<T> implements Comparator<Sortable<T>> {

    @Override
    public int compare(Sortable<T> a, Sortable<T> b) {
        return (a.priority() > b.priority() ? -1 : (a.priority() == b.priority() ? 0 : 1));
    }
}