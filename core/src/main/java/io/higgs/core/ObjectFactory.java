package io.higgs.core;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ObjectFactory {
    Object newInstance(Class<?> klass);
}
