package io.higgs.core;

import java.lang.reflect.Method;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface MethodProcessor {
    /**
     * Given the set of parameters, construct an {@link InvokableMethod}
     *
     * @param method    the Java class method which is to be processed
     * @param klass     the class to which the method belongs
     * @param factories a factories, which are registered with the server
     * @return an invokable method
     */
    InvokableMethod process(Method method, Class<?> klass, Queue<ObjectFactory> factories);
}
