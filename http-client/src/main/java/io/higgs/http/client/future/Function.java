package io.higgs.http.client.future;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface Function<T> {
  abstract   void apply(T data);
}
