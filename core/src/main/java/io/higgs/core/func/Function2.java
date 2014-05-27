package io.higgs.core.func;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface Function2<A, B, C> {
    C apply(A a, B b);
}
