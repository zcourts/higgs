package io.higgs.http.server.providers.filters;

import java.util.NavigableSet;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface HiggsFilterChain {
    /**
     * Get all the filters available in this chain
     *
     * @return set of available filters
     */
    NavigableSet<HiggsFilter> getFilters();

    /**
     * Get the next {@link io.higgs.http.server.providers.filters.HiggsFilter} in this chain.
     * The next filter is defined as a filter whose {@link HiggsFilter#priority()} is less than or equal to the
     * priority of the {@link #current()} filter. i.e. {@link #next()} traverses the available filters in descending
     * order of {@link HiggsFilter#priority()}.
     *
     * @return the next filter in the chain or null if none is available
     */
    HiggsFilter next();

    /**
     * The previous filter in this chain. If there is no previous filter and the chain has at least one filter then
     * the first filter in the chain is returned, subsequent calls from the first filter returns null.
     * Assuming {@link #next()} has been called a few times this returns the filter with a
     * {@link HiggsFilter#priority()} less than or equal to the {@link #current()} filter or null if the call would
     * traverse beyond the first filter
     *
     * @return the previous filter in this chain
     */
    HiggsFilter previous();

    /**
     * Gets the current filter. If neither {@link #next()} or {@link #previous()} have been called then this returns
     * the first filter in this chain if {@link #size()} > 0
     *
     * @return the current filter
     */
    HiggsFilter current();

    /**
     * Gets the number of filters in this chain
     *
     * @return the number of filters
     */
    int size();
}
