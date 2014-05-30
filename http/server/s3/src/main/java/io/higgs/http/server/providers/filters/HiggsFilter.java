package io.higgs.http.server.providers.filters;

import io.higgs.core.Sortable;
import io.higgs.http.server.HttpRequest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Provider
public interface HiggsFilter extends Sortable<HiggsFilter> {
    /**
     * Manipulate the request, prevent it from executing or perform some other action based on the contents of the
     * request.
     * The filter chain represents a set of filters that have been registered to process the request. Each filter is
     * responsible for calling the {@link #filterRequest(io.higgs.http.server.HttpRequest, DefaultFilterChain)} method
     * of next filter in the chain. Each filter can obtain the next filter by calling {@link HiggsFilterChain#next()}
     * on the chain.
     * <p/>
     * The first filter in the chain to return a non-null value should have it's response returned and the next filter
     * should not be called.
     * <p/>
     * A non-null response will be treated as the entity to be returned as the HTTP response to the request.
     *
     * @param request the request that has been made and is to be filtered
     * @param chain   the filter chain to be applied to the request
     * @return null if the request should proceed or a response to send as the HTTP response
     */
    Response filterRequest(HttpRequest request, DefaultFilterChain chain);
}
