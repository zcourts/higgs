package io.higgs.core;

import java.lang.reflect.Method;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface MethodProcessor {
    /**
     * Given the set of parameters, construct an {@link InvokableMethod}
     *
     * @param method  the Java class method which is to be processed
     * @param klass   the class to which the method belongs
     * @param factory a factory (or null), if available, which produces instances of the class
     * @return an invokable method
     */
    InvokableMethod process(Method method, Class<?> klass, ObjectFactory factory);
}
