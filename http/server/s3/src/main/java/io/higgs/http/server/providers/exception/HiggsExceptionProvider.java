package io.higgs.http.server.providers.exception;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Adds the {@link javax.ws.rs.ext.Provider} annotation so that all implementations are automatically registered.
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Provider
public interface HiggsExceptionProvider<T extends Throwable> extends ExceptionMapper<T> {
}
