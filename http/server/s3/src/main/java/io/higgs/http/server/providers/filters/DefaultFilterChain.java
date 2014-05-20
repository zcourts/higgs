package io.higgs.http.server.providers.filters;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultFilterChain implements HiggsFilterChain {
    protected final NavigableSet<HiggsFilter> filters;
    protected HiggsFilter current;

    public DefaultFilterChain() {
        filters = new ConcurrentSkipListSet<>(new Comparator<HiggsFilter>() {
            @Override
            public int compare(HiggsFilter o1, HiggsFilter o2) {
                return o2.priority() - o1.priority();
            }
        });
    }

    @Override
    public NavigableSet<HiggsFilter> getFilters() {
        return filters;
    }

    @Override
    public HiggsFilter next() {
        if (definedLast()) {
            return current;
        }
        return filters.floor(current);
    }

    @Override
    public HiggsFilter previous() {
        if (definedLast()) {
            return current;
        }
        return filters.ceiling(current);
    }

    @Override
    public HiggsFilter current() {
        if (definedLast()) {
            return current;
        }
        return current;
    }

    @Override
    public int size() {
        return filters.size();
    }

    private boolean definedLast() {
        Iterator<HiggsFilter> iterator = filters.iterator();
        if (current == null && iterator.hasNext()) {
            current = iterator.next();
            return true;
        }
        return false;
    }
}
