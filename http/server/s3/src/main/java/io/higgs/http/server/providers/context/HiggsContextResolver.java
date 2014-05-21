package io.higgs.http.server.providers.context;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Automatically register implementations as a {@link javax.ws.rs.ext.Provider}
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Provider
public interface HiggsContextResolver<T> extends ContextResolver<T> {
}
