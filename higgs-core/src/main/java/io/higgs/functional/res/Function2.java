package io.higgs.functional.res;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface Function2<R, T, T2> {
    R apply(T a, T2 b);
}
