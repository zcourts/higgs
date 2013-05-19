package io.higgs.http.client.future;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface Function<T> {
    void apply(T data);
}
